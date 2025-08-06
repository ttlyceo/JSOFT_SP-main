/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 *
 */
package l2e.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.ai.character.SummonAI;
import l2e.gameserver.ai.model.CtrlIntention;
import l2e.gameserver.data.holder.CharSummonHolder;
import l2e.gameserver.data.holder.SummonEffectsHolder;
import l2e.gameserver.data.parser.ItemsParser;
import l2e.gameserver.data.parser.PetsParser;
import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.handler.itemhandlers.IItemHandler;
import l2e.gameserver.handler.itemhandlers.ItemHandler;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.instancemanager.CursedWeaponsManager;
import l2e.gameserver.model.GameObject;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.PetData;
import l2e.gameserver.model.TimeStamp;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.Summon;
import l2e.gameserver.model.actor.stat.PetStat;
import l2e.gameserver.model.actor.templates.PetLevelTemplate;
import l2e.gameserver.model.actor.templates.items.Item;
import l2e.gameserver.model.actor.templates.items.Weapon;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.model.items.itemcontainer.Inventory;
import l2e.gameserver.model.items.itemcontainer.PcInventory;
import l2e.gameserver.model.items.itemcontainer.PetInventory;
import l2e.gameserver.model.skills.Skill;
import l2e.gameserver.model.skills.effects.Effect;
import l2e.gameserver.model.stats.BaseStats;
import l2e.gameserver.model.stats.Stats;
import l2e.gameserver.model.zone.ZoneId;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.InventoryUpdate;
import l2e.gameserver.network.serverpackets.PetInventoryUpdate;
import l2e.gameserver.network.serverpackets.PetItemList;
import l2e.gameserver.network.serverpackets.StatusUpdate;
import l2e.gameserver.network.serverpackets.StopMove;
import l2e.gameserver.network.serverpackets.SystemMessage;
import l2e.gameserver.taskmanager.DecayTaskManager;

public class PetInstance extends Summon
{
	private static final String ADD_SKILL_SAVE = "INSERT INTO character_pet_skills_save (petObjItemId,skill_id,skill_level,effect_count,effect_cur_time,effect_total_time,buff_index) VALUES (?,?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT petObjItemId,skill_id,skill_level,effect_count,effect_cur_time,effect_total_time,buff_index FROM character_pet_skills_save WHERE petObjItemId=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_pet_skills_save WHERE petObjItemId=?";
	
	private final Map<Integer, TimeStamp> _reuseTimeStampsSkills = new ConcurrentHashMap<>();
	private final Map<Integer, TimeStamp> _reuseTimeStampsItems = new ConcurrentHashMap<>();
	
	private int _curFed;
	private final PetInventory _inventory;
	private final int _controlObjectId;
	private boolean _respawned;
	private final boolean _mountable;
	private Future<?> _feedTask;
	private PetData _data;
	private PetLevelTemplate _leveldata;

	private long _expBeforeDeath = 0;
	private int _curWeightPenalty = 0;

	public final PetLevelTemplate getPetLevelData()
	{
		if (_leveldata == null)
		{
			_leveldata = PetsParser.getInstance().getPetLevelData(getTemplate().getId(), getStat().getLevel());
		}

		return _leveldata;
	}

	public final PetData getPetData()
	{
		if (_data == null)
		{
			_data = PetsParser.getInstance().getPetData(getTemplate().getId());
		}

		return _data;
	}

	public final void setPetData(PetLevelTemplate value)
	{
		_leveldata = value;
	}

	class FeedTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if ((getOwner() == null) || !getOwner().hasSummon() || (getOwner().getSummon().getObjectId() != getObjectId()))
				{
					stopFeed();
					return;
				}
				else if (getCurrentFed() > getFeedConsume())
				{
					setCurrentFed(getCurrentFed() - getFeedConsume());
				}
				else
				{
					setCurrentFed(0);
				}

				broadcastStatusUpdate();

				final List<Integer> foodIds = getPetData().getFood();
				if (foodIds.isEmpty())
				{
					if (isUncontrollable())
					{
						if ((getTemplate().getId() == 16050) && (getOwner() != null))
						{
							getOwner().setPkKills(Math.max(0, getOwner().getPkKills() - Rnd.get(1, 6)));
						}
						sendPacket(SystemMessageId.THE_HELPER_PET_LEAVING);
						deleteMe(getOwner());
					}
					else if (isHungry())
					{
						sendPacket(SystemMessageId.THERE_NOT_MUCH_TIME_REMAINING_UNTIL_HELPER_LEAVES);
					}
					return;
				}
				ItemInstance food = null;
				for (final int id : foodIds)
				{
					food = getInventory().getItemByItemId(id);
					if (food != null)
					{
						break;
					}
				}
				
				if ((food != null) && isHungry())
				{
					final IItemHandler handler = ItemHandler.getInstance().getHandler(food.getEtcItem());
					if (handler != null)
					{
						final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY);
						sm.addItemName(food.getId());
						sendPacket(sm);
						handler.useItem(PetInstance.this, food, false);
					}
				}
				
				if (isUncontrollable())
				{
					sendPacket(SystemMessageId.YOUR_PET_IS_STARVING_AND_WILL_NOT_OBEY_UNTIL_IT_GETS_ITS_FOOD_FEED_YOUR_PET);
				}
			}
			catch (final Exception e)
			{
				_log.error("Pet [ObjectId: " + getObjectId() + "] a feed task error has occurred", e);
			}
		}

		private int getFeedConsume()
		{
			if (isAttackingNow())
			{
				return getPetLevelData().getPetFeedBattle();
			}
			return getPetLevelData().getPetFeedNormal();
		}
	}

	public static PetInstance spawnPet(NpcTemplate template, Player owner, ItemInstance control)
	{
		if (GameObjectsStorage.getSummon(owner.getObjectId()) != null)
		{
			return null;
		}

		final PetData data = PetsParser.getInstance().getPetData(template.getId());

		final PetInstance pet = restore(control, template, owner);

		if (pet != null)
		{
			pet.setGlobalTitle(owner.getName(null));
			if (data.isSynchLevel() && (pet.getLevel() != owner.getLevel()))
			{
				pet.getStat().setLevel((byte) owner.getLevel());
				final long oldexp = pet.getStat().getExp();
				final long newexp = pet.getStat().getExpForLevel(owner.getLevel());
				if (oldexp > newexp)
				{
					pet.getStat().removeExp(oldexp - newexp);
				}
				else if (oldexp < newexp)
				{
					pet.getStat().addExp(newexp - oldexp);
				}
			}
		}
		return pet;
	}

	public PetInstance(int objectId, NpcTemplate template, Player owner, ItemInstance control)
	{
		this(objectId, template, owner, control, (byte) ((template.getIdTemplate() == 12564) || (template.getIdTemplate() == 16043) || (template.getIdTemplate() == 16044) || (template.getIdTemplate() == 16045) || (template.getIdTemplate() == 16046) || (template.getIdTemplate() == 16050) || (template.getIdTemplate() == 16051) || (template.getIdTemplate() == 16052) || (template.getIdTemplate() == 16053) ? owner.getLevel() : template.getLevel()));
	}

	public PetInstance(int objectId, NpcTemplate template, Player owner, ItemInstance control, int level)
	{
		super(objectId, template, owner);
		setInstanceType(InstanceType.PetInstance);

		_controlObjectId = control.getObjectId();

		getStat().setLevel((byte) Math.max(level, PetsParser.getInstance().getPetMinLevel(template.getId())));

		if ((template.getIdTemplate() == 16043) || (template.getIdTemplate() == 16044) || (template.getIdTemplate() == 16045) || (template.getIdTemplate() == 16046) || (template.getIdTemplate() == 16050) || (template.getIdTemplate() == 16051) || (template.getIdTemplate() == 16052) || (template.getIdTemplate() == 16053))
		{
			getStat().setLevel((byte) getOwner().getLevel());
		}

		_inventory = new PetInventory(this);
		_inventory.restore();

		final int npcId = template.getId();
		_mountable = PetsParser.isMountable(npcId);
		getPetData();
		getPetLevelData();
	}

	@Override
	public PetStat getStat()
	{
		return (PetStat) super.getStat();
	}

	@Override
	public void initCharStat()
	{
		setStat(new PetStat(this));
	}

	public boolean isRespawned()
	{
		return _respawned;
	}

	@Override
	public int getSummonType()
	{
		return 2;
	}

	@Override
	public int getControlObjectId()
	{
		return _controlObjectId;
	}

	public ItemInstance getControlItem()
	{
		return getOwner().getInventory().getItemByObjectId(_controlObjectId);
	}

	public int getCurrentFed()
	{
		return _curFed;
	}

	public void setCurrentFed(int num)
	{
		_curFed = num > getMaxFed() ? getMaxFed() : num;
	}

	@Override
	public ItemInstance getActiveWeaponInstance()
	{
		for (final ItemInstance item : getInventory().getItems())
		{
			if ((item.getItemLocation() == ItemInstance.ItemLocation.PET_EQUIP) && (item.getItem().getBodyPart() == Item.SLOT_R_HAND))
			{
				return item;
			}
		}
		return null;
	}

	@Override
	public Weapon getActiveWeaponItem()
	{
		final ItemInstance weapon = getActiveWeaponInstance();

		if (weapon == null)
		{
			return null;
		}

		return (Weapon) weapon.getItem();
	}

	@Override
	public ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}

	@Override
	public PetInventory getInventory()
	{
		return _inventory;
	}

	@Override
	public boolean destroyItem(String process, int objectId, long count, GameObject reference, boolean sendMessage)
	{
		final ItemInstance item = _inventory.destroyItem(process, objectId, count, getOwner(), reference);

		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			}

			return false;
		}

		final PetInventoryUpdate petIU = new PetInventoryUpdate();
		petIU.addItem(item);
		sendPacket(petIU);

		if (sendMessage)
		{
			if (count > 1)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
				sm.addItemName(item.getId());
				sm.addItemNumber(count);
				sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
				sm.addItemName(item.getId());
				sendPacket(sm);
			}
		}
		return true;
	}

	@Override
	public boolean destroyItemByItemId(String process, int itemId, long count, GameObject reference, boolean sendMessage)
	{
		final ItemInstance item = _inventory.destroyItemByItemId(process, itemId, count, getOwner(), reference);

		if (item == null)
		{
			if (sendMessage)
			{
				sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			}
			return false;
		}
		final PetInventoryUpdate petIU = new PetInventoryUpdate();
		petIU.addItem(item);
		sendPacket(petIU);

		if (sendMessage)
		{
			if (count > 1)
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
				sm.addItemName(item.getId());
				sm.addItemNumber(count);
				sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
				sm.addItemName(item.getId());
				sendPacket(sm);
			}
		}

		return true;
	}

	@Override
	public void doPickupItem(GameObject object)
	{
		final boolean follow = isInFollowStatus();
		if (isDead())
		{
			return;
		}
		getAI().setIntention(CtrlIntention.IDLE);
		final StopMove sm = new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading());

		if (Config.DEBUG)
		{
			_log.info("Pet pickup pos: " + object.getX() + " " + object.getY() + " " + object.getZ());
		}

		broadcastPacket(sm);

		if (!(object instanceof ItemInstance))
		{
			_log.warn(this + " trying to pickup wrong target." + object);
			sendActionFailed();
			return;
		}

		final ItemInstance target = (ItemInstance) object;

		if (CursedWeaponsManager.getInstance().isCursed(target.getId()))
		{
			final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
			smsg.addItemName(target.getId());
			sendPacket(smsg);
			return;
		}

		synchronized (target)
		{
			if (!target.isVisible())
			{
				sendActionFailed();
				return;
			}
			if (!target.getDropProtection().tryPickUp(this))
			{
				sendActionFailed();
				final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
				smsg.addItemName(target);
				sendPacket(smsg);
				return;
			}
			if (!_inventory.validateCapacity(target))
			{
				sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_ANY_MORE_ITEMS);
				return;
			}
			if (!_inventory.validateWeight(target, target.getCount()))
			{
				sendPacket(SystemMessageId.UNABLE_TO_PLACE_ITEM_YOUR_PET_IS_TOO_ENCUMBERED);
				return;
			}
			if ((target.getOwnerId() != 0) && (target.getOwnerId() != getOwner().getObjectId()) && !getOwner().isInLooterParty(target.getOwnerId()))
			{
				sendActionFailed();

				if (target.getId() == PcInventory.ADENA_ID)
				{
					final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA);
					smsg.addItemNumber(target.getCount());
					sendPacket(smsg);
				}
				else if (target.getCount() > 1)
				{
					final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S);
					smsg.addItemName(target.getId());
					smsg.addItemNumber(target.getCount());
					sendPacket(smsg);
				}
				else
				{
					final SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
					smsg.addItemName(target.getId());
					sendPacket(smsg);
				}

				return;
			}
			target.pickupMe(this);
		}

		if (target.getItem().isHerb())
		{
			final IItemHandler handler = ItemHandler.getInstance().getHandler(target.getEtcItem());
			if (handler == null)
			{
				_log.warn("No item handler registered for item ID: " + target.getId() + ".");
			}
			else
			{
				handler.useItem(this, target, false);
			}

			ItemsParser.getInstance().destroyItem("Consume", target, getOwner(), null);

			broadcastStatusUpdate();
		}
		else
		{
			if (target.getId() == PcInventory.ADENA_ID)
			{
				final SystemMessage sm2 = SystemMessage.getSystemMessage(SystemMessageId.PET_PICKED_S1_ADENA);
				sm2.addItemNumber(target.getCount());
				sendPacket(sm2);
			}
			else if (target.getEnchantLevel() > 0)
			{
				final SystemMessage sm2 = SystemMessage.getSystemMessage(SystemMessageId.PET_PICKED_S1_S2);
				sm2.addNumber(target.getEnchantLevel());
				sm2.addString(target.getName(null));
				sendPacket(sm2);
			}
			else if (target.getCount() > 1)
			{
				final SystemMessage sm2 = SystemMessage.getSystemMessage(SystemMessageId.PET_PICKED_S2_S1_S);
				sm2.addItemNumber(target.getCount());
				sm2.addString(target.getName(null));
				sendPacket(sm2);
			}
			else
			{
				final SystemMessage sm2 = SystemMessage.getSystemMessage(SystemMessageId.PET_PICKED_S1);
				sm2.addString(target.getName(null));
				sendPacket(sm2);
			}
			
			if (getOwner().isInParty() && (getOwner().getParty().getLootDistribution() != Party.ITEM_LOOTER))
			{
				getOwner().getParty().distributeItem(getOwner(), target);
			}
			else
			{
				getInventory().addItem("Pickup", target, getOwner(), this);
				sendPacket(new PetItemList(getInventory().getItems()));
			}
		}

		getAI().setIntention(CtrlIntention.IDLE);

		if (follow)
		{
			((SummonAI) getAI()).setStartFollowController(true);
			followOwner();
		}
	}

	@Override
	public void deleteMe(Player owner)
	{
		getInventory().transferItemsToOwner();
		super.deleteMe(owner);
		destroyControlItem(owner, false);
		CharSummonHolder.getInstance().getPets().remove(getOwner().getObjectId());
	}

	@Override
	protected void onDeath(Creature killer)
	{
		stopFeed();
		sendPacket(SystemMessageId.MAKE_SURE_YOU_RESSURECT_YOUR_PET_WITHIN_24_HOURS);
		DecayTaskManager.getInstance().addDecayTask(this, 86400000L, true);

		final Player owner = getOwner();
		if ((owner != null) && !owner.isInDuel() && (!isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.SIEGE)))
		{
			deathPenalty();
		}
		super.onDeath(killer);
	}

	@Override
	public void doRevive()
	{
		getOwner().removeReviving();
		stopDecay();
		super.doRevive();

		startFeed();
		if (!isHungry())
		{
			setRunning();
		}
		getAI().setIntention(CtrlIntention.ACTIVE, null);
	}

	@Override
	public void doRevive(double revivePower)
	{
		restoreExp(revivePower);
		doRevive();
	}

	public ItemInstance transferItem(String process, int objectId, long count, Inventory target, Player actor, GameObject reference)
	{
		final ItemInstance oldItem = getInventory().getItemByObjectId(objectId);
		final ItemInstance playerOldItem = target.getItemByItemId(oldItem.getId());
		final ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, actor, reference);

		if (newItem == null)
		{
			return null;
		}

		final PetInventoryUpdate petIU = new PetInventoryUpdate();
		if ((oldItem.getCount() > 0) && (oldItem != newItem))
		{
			petIU.addModifiedItem(oldItem);
		}
		else
		{
			petIU.addRemovedItem(oldItem);
		}
		sendPacket(petIU);

		if (!newItem.isStackable())
		{
			if (getOwner() != null)
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addNewItem(newItem);
				getOwner().sendPacket(iu);
			}
		}
		else if ((playerOldItem != null) && newItem.isStackable())
		{
			if (getOwner() != null)
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(newItem);
				getOwner().sendPacket(iu);
			}
		}

		return newItem;
	}

	public void destroyControlItem(Player owner, boolean evolve)
	{
		try
		{
			ItemInstance removedItem;
			if (evolve)
			{
				removedItem = owner.getInventory().destroyItem("Evolve", getControlObjectId(), 1, getOwner(), this);
			}
			else
			{
				removedItem = owner.getInventory().destroyItem("PetDestroy", getControlObjectId(), 1, getOwner(), this);
				if (removedItem != null)
				{
					final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
					sm.addItemName(removedItem);
					owner.sendPacket(sm);
				}
			}

			if (removedItem == null)
			{
				_log.warn("Couldn't destroy pet control item for " + owner + " pet: " + this + " evolve: " + evolve);
			}
			else
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addRemovedItem(removedItem);
				owner.sendPacket(iu);
				owner.sendStatusUpdate(false, false, StatusUpdate.CUR_LOAD);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error while destroying control item: " + e.getMessage(), e);
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id = ?");
			statement.setInt(1, getControlObjectId());
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.error("Failed to delete Pet [ObjectId: " + getObjectId() + "]", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void dropAllItems()
	{
		try
		{
			for (final ItemInstance item : getInventory().getItems())
			{
				dropItemHere(item);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Pet Drop Error: " + e.getMessage(), e);
		}
	}

	public void dropItemHere(ItemInstance dropit, boolean protect)
	{
		dropit = getInventory().dropItem("Drop", dropit.getObjectId(), dropit.getCount(), getOwner(), this);

		if (dropit != null)
		{
			if (protect)
			{
				dropit.getDropProtection().protect(getOwner(), false);
			}
			_log.info("Item id to drop: " + dropit.getId() + " amount: " + dropit.getCount());
			dropit.dropMe(this, getX(), getY(), getZ() + 100, true);
		}
	}

	public void dropItemHere(ItemInstance dropit)
	{
		dropItemHere(dropit, false);
	}

	@Override
	public boolean isMountable()
	{
		return _mountable;
	}

	private static PetInstance restore(ItemInstance control, NpcTemplate template, Player owner)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT item_obj_id, name, level, curHp, curMp, exp, sp, fed FROM pets WHERE item_obj_id=?");
			PetInstance pet;
			statement.setInt(1, control.getObjectId());
			rset = statement.executeQuery();
			final int id = IdFactory.getInstance().getNextId();
			if (!rset.next())
			{
				if (template.isType("BabyPet"))
				{
					pet = new BabyPetInstance(id, template, owner, control);
				}
				else
				{
					pet = new PetInstance(id, template, owner, control);
				}
				return pet;
			}

			if (template.isType("BabyPet"))
			{
				pet = new BabyPetInstance(id, template, owner, control, rset.getByte("level"));
			}
			else
			{
				pet = new PetInstance(id, template, owner, control, rset.getByte("level"));
			}

			pet._respawned = true;
			pet.setGlobalName(rset.getString("name") != null ? rset.getString("name").isEmpty() ? "" : rset.getString("name") : "");

			long exp = rset.getLong("exp");
			final PetLevelTemplate info = PetsParser.getInstance().getPetLevelData(pet.getId(), pet.getLevel());

			if ((info != null) && (exp < info.getPetMaxExp()))
			{
				exp = info.getPetMaxExp();
			}

			pet.getStat().setExp(exp);
			pet.getStat().setSp(rset.getInt("sp"));

			pet.getStatus().setCurrentHp(rset.getInt("curHp"));
			pet.getStatus().setCurrentMp(rset.getInt("curMp"));
			pet.getStatus().setCurrentCp(pet.getMaxCp());
			if (rset.getDouble("curHp") < 1)
			{
				pet.setIsDead(true);
				pet.stopHpMpRegeneration();
			}
			pet.setCurrentFed(rset.getInt("fed"));
			return pet;
		}
		catch (final Exception e)
		{
			_log.warn("Could not restore pet data for owner: " + owner + " - " + e.getMessage(), e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return null;
	}

	@Override
	public void setRestoreSummon(boolean val)
	{
		_restoreSummon = val;
	}

	@Override
	public final void stopSkillEffects(int skillId)
	{
		super.stopSkillEffects(skillId);
		SummonEffectsHolder.getInstance().removePetEffects(getControlObjectId(), skillId);
	}

	@Override
	public void store()
	{
		if (getControlObjectId() == 0)
		{
			return;
		}

		super.store();
		
		if (!Config.RESTORE_PET_ON_RECONNECT)
		{
			_restoreSummon = false;
		}

		String req;
		if (!isRespawned())
		{
			req = "INSERT INTO pets (name,level,curHp,curMp,exp,sp,fed,ownerId,restore,item_obj_id) " + "VALUES (?,?,?,?,?,?,?,?,?,?)";
		}
		else
		{
			req = "UPDATE pets SET name=?,level=?,curHp=?,curMp=?,exp=?,sp=?,fed=?,ownerId=?,restore=? " + "WHERE item_obj_id = ?";
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(req);
			statement.setString(1, !isRespawned() ? "" : getName(null));
			statement.setInt(2, getStat().getLevel());
			statement.setDouble(3, getStatus().getCurrentHp());
			statement.setDouble(4, getStatus().getCurrentMp());
			statement.setLong(5, getStat().getExp());
			statement.setInt(6, getStat().getSp());
			statement.setInt(7, getCurrentFed());
			statement.setInt(8, getOwner().getObjectId());
			statement.setInt(9, _restoreSummon ? 1 : 0);
			statement.setInt(10, getControlObjectId());

			statement.executeUpdate();
			statement.close();
			
			if (!isRespawned())
			{
				setGlobalName("");
			}
			_respawned = true;

			if (_restoreSummon)
			{
				CharSummonHolder.getInstance().getPets().put(getOwner().getObjectId(), getControlObjectId());
			}
			else
			{
				CharSummonHolder.getInstance().getPets().remove(getOwner().getObjectId());
			}
		}
		catch (final Exception e)
		{
			_log.error("Failed to store Pet [ObjectId: " + getObjectId() + "] data", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		
		final var itemInst = getControlItem();
		if ((itemInst != null) && (itemInst.getEnchantLevel() != getStat().getLevel()))
		{
			itemInst.setEnchantLevel(getStat().getLevel());
			itemInst.updateDatabase();
		}
	}

	@Override
	public void storeEffect(boolean storeEffects)
	{
		if (!Config.SUMMON_STORE_SKILL_COOLTIME)
		{
			return;
		}

		SummonEffectsHolder.getInstance().clearPetEffects(getControlObjectId());

		Connection con = null;
		PreparedStatement ps = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			ps = con.prepareStatement(DELETE_SKILL_SAVE);
			ps.setInt(1, getControlObjectId());
			ps.execute();
			ps.close();
			
			ps = con.prepareStatement(ADD_SKILL_SAVE);
			int buff_index = 0;

			final List<Integer> storedSkills = new LinkedList<>();

			if (storeEffects)
			{
				for (final Effect effect : getAllEffects())
				{
					if (effect == null)
					{
						continue;
					}

					switch (effect.getEffectType())
					{
						case HEAL_OVER_TIME :
						case CPHEAL_OVER_TIME :
						case HIDE :
							continue;
					}

					if (effect.getAbnormalType().equalsIgnoreCase("LIFE_FORCE_OTHERS"))
					{
						continue;
					}

					final Skill skill = effect.getSkill();
					
					if (skill.isToggle() || skill.isReflectionBuff())
					{
						continue;
					}
					
					if (skill.isDance() && !Config.ALT_STORE_DANCES)
					{
						continue;
					}

					if (storedSkills.contains(skill.getReuseHashCode()))
					{
						continue;
					}

					storedSkills.add(skill.getReuseHashCode());

					if (effect.isInUse())
					{
						ps.setInt(1, getControlObjectId());
						ps.setInt(2, skill.getId());
						ps.setInt(3, skill.getLevel());
						ps.setInt(4, effect.getTickCount());
						ps.setInt(5, effect.getTime());
						ps.setInt(6, effect.getAbnormalTime());
						ps.setInt(7, ++buff_index);
						ps.addBatch();

						SummonEffectsHolder.getInstance().addPetEffect(getControlObjectId(), skill, effect.getTickCount(), effect.getTime(), effect.getAbnormalTime());
					}
				}
				ps.executeBatch();
			}
		}
		catch (final Exception e)
		{
			_log.warn("Could not store pet effect data: ", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, ps);
		}
	}

	@Override
	public void restoreEffects()
	{
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			ps = con.prepareStatement(RESTORE_SKILL_SAVE);
			if (!SummonEffectsHolder.getInstance().containsPetId(getControlObjectId()))
			{
				ps.setInt(1, getControlObjectId());
				rset = ps.executeQuery();
				while (rset.next())
				{
					final int effectCount = rset.getInt("effect_count");
					final int effectCurTime = rset.getInt("effect_cur_time");
					final int effectTotalTime = rset.getInt("effect_total_time");
					
					final Skill skill = SkillsParser.getInstance().getInfo(rset.getInt("skill_id"), rset.getInt("skill_level"));
					if (skill == null || skill.isReflectionBuff())
					{
						continue;
					}

					if (skill.hasEffects())
					{
						SummonEffectsHolder.getInstance().addPetEffect(getControlObjectId(), skill, effectCount, effectCurTime, effectTotalTime);
					}
				}
			}
			ps.close();

			ps = con.prepareStatement(DELETE_SKILL_SAVE);
			ps.setInt(1, getControlObjectId());
			ps.executeUpdate();
		}
		catch (final Exception e)
		{
			_log.warn("Could not restore " + this + " active effect data: " + e.getMessage(), e);
		}
		finally
		{
			SummonEffectsHolder.getInstance().applyPetEffects(this, getControlObjectId());
			DbUtils.closeQuietly(con, ps, rset);
		}
	}

	public void stopFeed()
	{
		if (_feedTask != null)
		{
			_feedTask.cancel(false);
			_feedTask = null;
		}
	}

	public void startFeed()
	{
		stopFeed();
		if (!isDead() && (getOwner().getSummon() == this))
		{
			_feedTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(new FeedTask(), 10000, 10000);
		}
	}

	@Override
	public void unSummon(Player owner)
	{
		stopFeed();
		super.unSummon(owner);

		if (!isDead())
		{
			if (getInventory() != null)
			{
				getInventory().deleteMe();
			}
		}
	}

	public void restoreExp(double restorePercent)
	{
		if (_expBeforeDeath > 0)
		{
			getStat().addExp(Math.round(((_expBeforeDeath - getStat().getExp()) * restorePercent) / 100));
			_expBeforeDeath = 0;
		}
	}

	private void deathPenalty()
	{
		final int lvl = getStat().getLevel();
		final double percentLost = (-0.07 * lvl) + 6.5;

		final long lostExp = Math.round(((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost) / 100);

		_expBeforeDeath = getStat().getExp();

		getStat().addExp(-lostExp);
	}

	@Override
	public void addExpAndSp(long addToExp, int addToSp)
	{
		if (getId() == 12564)
		{
			getStat().addExpAndSp(Math.round(addToExp * Config.SINEATER_XP_RATE), addToSp);
		}
		else
		{
			getStat().addExpAndSp(Math.round(addToExp * Config.PET_XP_RATE), addToSp);
		}
	}

	@Override
	public long getExpForThisLevel()
	{
		return getStat().getExpForLevel(getLevel());
	}

	@Override
	public long getExpForNextLevel()
	{
		return getStat().getExpForLevel(getLevel() + 1);
	}

	@Override
	public final int getLevel()
	{
		return getStat().getLevel();
	}

	public int getMaxFed()
	{
		return getStat().getMaxFeed();
	}

	@Override
	public double getMAtk(Creature target, Skill skill)
	{
		return getStat().getMAtk(target, skill) + (getOwner().getMAtk(target, skill) * (getOwner().getPetShareBonus(Stats.MAGIC_ATTACK) - 1.0));
	}
	
	@Override
	public double getMDef(Creature target, Skill skill)
	{
		return getStat().getMDef(target, skill) + (getOwner().getMDef(target, skill) * (getOwner().getPetShareBonus(Stats.MAGIC_DEFENCE) - 1.0));
	}
	
	@Override
	public double getPAtk(Creature target)
	{
		return getStat().getPAtk(target) + (getOwner().getPAtk(target) * (getOwner().getPetShareBonus(Stats.POWER_ATTACK) - 1.0));
	}
	
	@Override
	public double getPDef(Creature target)
	{
		return getStat().getPDef(target) + (getOwner().getPDef(target) * (getOwner().getPetShareBonus(Stats.POWER_DEFENCE) - 1.0));
	}
	
	@Override
	public double getMAtkSpd()
	{
		return getStat().getMAtkSpd() + (getOwner().getMAtkSpd() * (getOwner().getPetShareBonus(Stats.MAGIC_ATTACK_SPEED) - 1.0));
	}
	
	@Override
	public double getCriticalHit(Creature target, Skill skill)
	{
		return getStat().getCriticalHit(target, skill) + ((getOwner().getCriticalHit(target, skill)) * (getOwner().getPetShareBonus(Stats.CRITICAL_RATE) - 1.0));
	}
	
	@Override
	public double getPAtkSpd()
	{
		return getStat().getPAtkSpd() + (getOwner().getPAtkSpd() * (getOwner().getPetShareBonus(Stats.POWER_ATTACK_SPEED) - 1.0));
	}

	@Override
	public final int getSkillLevel(int skillId)
	{
		if (getKnownSkill(skillId) == null)
		{
			return -1;
		}

		final int lvl = getLevel();
		return lvl > 70 ? 7 + ((lvl - 70) / 5) : lvl / 10;
	}

	public void updateRefOwner(Player owner)
	{
		setOwner(owner);
		GameObjectsStorage.remove(this);
		GameObjectsStorage.put(this);
	}

	public int getInventoryLimit()
	{
		return Config.INVENTORY_MAXIMUM_PET;
	}

	public void refreshOverloaded()
	{
		final int maxLoad = getMaxLoad();
		if (maxLoad > 0)
		{
			final long weightproc = (((getCurrentLoad() - getBonusWeightPenalty()) * 1000) / maxLoad);
			int newWeightPenalty;
			if ((weightproc < 500) || getOwner().getDietMode())
			{
				newWeightPenalty = 0;
			}
			else if (weightproc < 666)
			{
				newWeightPenalty = 1;
			}
			else if (weightproc < 800)
			{
				newWeightPenalty = 2;
			}
			else if (weightproc < 1000)
			{
				newWeightPenalty = 3;
			}
			else
			{
				newWeightPenalty = 4;
			}

			if (_curWeightPenalty != newWeightPenalty)
			{
				_curWeightPenalty = newWeightPenalty;
				if (newWeightPenalty > 0)
				{
					addSkill(SkillsParser.getInstance().getInfo(4270, newWeightPenalty));
					setIsOverloaded(getCurrentLoad() >= maxLoad);
				}
				else
				{
					removeSkill(getKnownSkill(4270), true);
					setIsOverloaded(false);
				}
			}
		}
	}

	@Override
	public void updateAndBroadcastStatus(int val)
	{
		refreshOverloaded();
		super.updateAndBroadcastStatus(val);
	}

	@Override
	public final boolean isHungry()
	{
		return getCurrentFed() < ((getPetData().getHungryLimit() / 100f) * getPetLevelData().getPetMaxFeed());
	}

	@Override
	public final int getWeapon()
	{
		final ItemInstance weapon = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (weapon != null)
		{
			return weapon.getId();
		}
		return 0;
	}

	@Override
	public final int getArmor()
	{
		final ItemInstance weapon = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if (weapon != null)
		{
			return weapon.getId();
		}
		return 0;
	}

	public final int getJewel()
	{
		final ItemInstance weapon = getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK);
		if (weapon != null)
		{
			return weapon.getId();
		}
		return 0;
	}

	@Override
	public int getSoulShotsPerHit()
	{
		return getPetLevelData().getPetSoulShot();
	}

	@Override
	public int getSpiritShotsPerHit()
	{
		return getPetLevelData().getPetSpiritShot();
	}

	@Override
	public void setName(String lang, String name)
	{
		final ItemInstance controlItem = getControlItem();
		if (controlItem != null)
		{
			if (controlItem.getCustomType2() == (name == null ? 1 : 0))
			{
				controlItem.setCustomType2(name != null ? 1 : 0);
				controlItem.updateDatabase();
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(controlItem);
				sendPacket(iu);
			}
		}
		else
		{
			_log.warn("Pet control item null, for pet: " + toString());
		}
		super.setName(lang, name);
	}

	public boolean canEatFoodId(int itemId)
	{
		return _data.getFood().contains(itemId);
	}

	public Map<Integer, TimeStamp> getSkillReuseTimeStamps()
	{
		return _reuseTimeStampsSkills;
	}

	@Override
	public void addTimeStamp(Skill skill, long reuse)
	{
		_reuseTimeStampsSkills.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse));
	}

	@Override
	public long getSkillRemainingReuseTime(int skillReuseHashId)
	{
		if (_reuseTimeStampsSkills.isEmpty() || !_reuseTimeStampsSkills.containsKey(skillReuseHashId))
		{
			return -1;
		}
		return _reuseTimeStampsSkills.get(skillReuseHashId).getRemaining();
	}

	@Override
	public void addTimeStampItem(ItemInstance item, long reuse, boolean byCron)
	{
		_reuseTimeStampsItems.put(item.getObjectId(), new TimeStamp(item, reuse, byCron));
	}

	@Override
	public long getItemRemainingReuseTime(int itemObjId)
	{
		if (_reuseTimeStampsItems.isEmpty() || !_reuseTimeStampsItems.containsKey(itemObjId))
		{
			return -1;
		}
		return _reuseTimeStampsItems.get(itemObjId).getRemaining();
	}

	@Override
	public boolean isPet()
	{
		return true;
	}
	
	@Override
	public int getMaxLoad()
	{
		return (int) calcStat(Stats.WEIGHT_LIMIT, Math.floor(BaseStats.CON.calcBonus(this) * 34500 * Config.ALT_WEIGHT_LIMIT), this, null);
	}
	
	@Override
	public int getBonusWeightPenalty()
	{
		return (int) calcStat(Stats.WEIGHT_PENALTY, 1, this, null);
	}
	
	@Override
	public int getCurrentLoad()
	{
		return getInventory().getTotalWeight();
	}
	
	@Override
	public double getLevelMod()
	{
		return (89. + getLevel()) / 100.0;
	}
	
	public boolean isUncontrollable()
	{
		return getCurrentFed() <= 0;
	}
}