<br1>
<center><font color="DBC309">Player Management:</font></center>
<table height=25>
    <tr>
        <td><center><edit var="player_cmd" width=273 height=15></center></td>
    </tr>
</table>
<table>
    <tr>
        <td><button value="Find" action="bypass -h admin_jg_find player $player_cmd" width=135 height=21 back="L2UI_ch3.bigbutton3_down" fore="L2UI_ch3.bigbutton3"></td>
        <td><button value="Ban HWID" action="bypass -h admin_jg_ban player $player_cmd" width=135 height=21 back="L2UI_ch3.bigbutton3_down" fore="L2UI_ch3.bigbutton3"></td>
    </tr>
</table>
<br>
<br>
<center><font color="DBC309">Actions with the player by his HWID:</font></center>
<table height=25>
    <tr>
        <td><center><edit var="hwid_cmd" width=273 height=15></center></td>
    </tr>
</table>

<table>
    <tr>
        <td><button value="Ban HWID" action="bypass -h admin_jg_ban hwid $hwid_cmd" width=135 height=21 back="L2UI_ch3.bigbutton3_down" fore="L2UI_ch3.bigbutton3"></td>
        <td><button value="Unban HWID" action="bypass -h admin_jg_unban $hwid_cmd" width=135 height=21 back="L2UI_ch3.bigbutton3_down" fore="L2UI_ch3.bigbutton3"></td>
    </tr>
</table>
<center><button value="Find" action="bypass -h admin_jg_find hwid $hwid_cmd" width=135 height=21 back="L2UI_ch3.bigbutton3_down" fore="L2UI_ch3.bigbutton3"></center>
<br>