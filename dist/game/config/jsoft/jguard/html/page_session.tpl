<table bgcolor=000000 width=285>
    <tr>
        <td><font color="777777">HWID:</font> %hwid%</td>
    </tr>
    <tr>
        <td><font color="777777">Accounts in game:</font> %online%</td>
    </tr>
	
    <tr>
        <td>
            <table width=285>
                <tr>
                    <td><font color="777777">Player</font></td>
                    <td><font color="777777">Account</font></td>
                    <td><font color="777777">Action</font></td>
                </tr>
                %records%
            </table>
        </td>
    </tr>
</table>
<br>
<table border=0>
    <tr>
        <td><button value="Ban HWID" action="bypass -h admin_jg_ban hwid %hwid%" width=135 height=21 back="L2UI_ch3.bigbutton3_down" fore="L2UI_ch3.bigbutton3"></td>
        <td><button value="Kick all" action="bypass -h admin_jg_kick_session %sid%" width=135 height=21 back="L2UI_ch3.bigbutton3_down" fore="L2UI_ch3.bigbutton3"></td>
    </tr>
</table>