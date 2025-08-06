package top.jsoft.jguard.crypt;


import org.strixplatform.network.cipher.StrixGameCrypt;
import top.jsoft.jguard.JGuard;

/**
 * @author psygrammator
 * @date 11.03.2022
 */
public class JGameCrypt extends StrixGameCrypt {
    private final byte[] inKey = new byte[16], outKey = new byte[16];
    private final JCrypt cryptIn = new JCrypt();
    private final JCrypt cryptOut = new JCrypt();
    private boolean isEnabled = false;

    @Override
    public void setKey(final byte[] key) {
        System.arraycopy(key, 0, inKey, 0, 16);
        System.arraycopy(key, 0, outKey, 0, 16);
        if (JGuard.isProtectEnabled()) {
            cryptIn.setKey(key);
            cryptOut.setKey(key);
        }
    }

    @Override
    public boolean decrypt(final byte[] raw, final int offset, final int size) {
        if (!isEnabled)
            return true;

        if (JGuard.isProtectEnabled()) {
            cryptIn.chiper(raw, offset, size);
            return true;
        }

        int temp = 0;
        for (int i = 0; i < size; i++)
        {
            int temp2 = raw[offset + i] & 0xFF;
            raw[offset + i] = (byte) (temp2 ^ inKey[i & 15] ^ temp);
            temp = temp2;
        }

        int old = inKey[8] & 0xff;
        old |= (inKey[9] << 8) & 0xff00;
        old |= (inKey[10] << 0x10) & 0xff0000;
        old |= (inKey[11] << 0x18) & 0xff000000;

        old += size;

        inKey[8] = (byte) (old & 0xff);
        inKey[9] = (byte) ((old >> 0x08) & 0xff);
        inKey[10] = (byte) ((old >> 0x10) & 0xff);
        inKey[11] = (byte) ((old >> 0x18) & 0xff);
        return true;
    }

    @Override
    public boolean encrypt(final byte[] raw, final int offset, final int size) {
        if (!isEnabled) {
            isEnabled = true;
            return true;
        }

        if (JGuard.isProtectEnabled()) {
            cryptOut.chiper(raw, offset, size);
            return true;
        }

        int temp = 0;
        for (int i = 0; i < size; i++)
        {
            int temp2 = raw[offset + i] & 0xFF;
            temp = temp2 ^ outKey[i & 15] ^ temp;
            raw[offset + i] = (byte) temp;
        }

        int old = outKey[8] & 0xff;
        old |= (outKey[9] << 8) & 0xff00;
        old |= (outKey[10] << 0x10) & 0xff0000;
        old |= (outKey[11] << 0x18) & 0xff000000;

        old += size;

        outKey[8] = (byte) (old & 0xff);
        outKey[9] = (byte) ((old >> 0x08) & 0xff);
        outKey[10] = (byte) ((old >> 0x10) & 0xff);
        outKey[11] = (byte) ((old >> 0x18) & 0xff);
        return true;
    }
}
