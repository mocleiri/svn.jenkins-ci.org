import com4j.COM4J;
import com4j.Variant;
import com4j.Com4jObject;
import com4j.typelibs.activeDirectory.IADs;
import com4j.typelibs.activeDirectory.IADsOpenDSObject;
import com4j.typelibs.activeDirectory.IADsUser;
import com4j.typelibs.activeDirectory.IADsGroup;
import com4j.typelibs.ado20.ClassFactory;
import com4j.typelibs.ado20._Command;
import com4j.typelibs.ado20._Connection;
import com4j.typelibs.ado20._Recordset;

/**
 * Connects to the Active Directory (of the domain that this machine is a member of, I suppose)
 * and find an user.
 *
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(String[] args) {
        _Connection con = ClassFactory.createConnection();
        con.provider("ADsDSOObject");
        con.open("Active Directory Provider",""/*default*/,""/*default*/,-1/*default*/);

        _Command cmd = ClassFactory.createCommand();
        cmd.activeConnection(con);

        IADs rootDSE = COM4J.getObject(IADs.class, "LDAP://RootDSE", null);

        String context = (String)rootDSE.get("defaultNamingContext");
        System.out.println("Context is "+context);

        cmd.commandText("<LDAP://"+context+">;(sAMAccountName="+args[0]+");name,description,distinguishedName;subTree");
        _Recordset rs = cmd.execute(null, Variant.MISSING, -1/*default*/);

        String dn=null;

        while(!rs.eof()) {
            System.out.println(rs.fields().item("Name").value());
            System.out.println(rs.fields().item("Description").value());
            dn = rs.fields().item("distinguishedName").value().toString();
            System.out.println(dn);
            rs.moveNext();
        }

        // now we got the DN of the user
        IADsOpenDSObject dso = COM4J.getObject(IADsOpenDSObject.class,"LDAP:",null);

        // turns out we don't need DN for authentication
        // we can bind with the user name
        // dso.openDSObject("LDAP://"+context,args[0],args[1],1);

        // to do bind with DN as the user name, the flag must be 0
        IADsUser usr = dso.openDSObject("LDAP://" + dn, dn, args[1], 0).queryInterface(IADsUser.class);
        System.out.println("Logged in as "+usr.fullName());
        for( Com4jObject g : usr.groups() ) {
            IADsGroup grp = g.queryInterface(IADsGroup.class);
            System.out.println("Member of: "+grp.name());
        }
    }
}

/*
' Create the connection and command object.
Set oConnection1 = CreateObject("ADODB.Connection")
Set oCommand1 = CreateObject("ADODB.Command")
' Open the connection.
oConnection1.Provider = "ADsDSOObject"  ' This is the ADSI OLE-DB provider name
oConnection1.Open "Active Directory Provider"
' Create a command object for this connection.
Set oCommand1.ActiveConnection = oConnection1

' Figure out what DN to connect to, by checking the current domain
Set rootDSE = GetObject("LDAP://RootDSE")

' Compose a search string.
' DCs need to be specified or else this fails.
' See http://msdn2.microsoft.com/en-us/library/aa746470(VS.85).aspx for the syntax
oCommand1.CommandText = "<LDAP://"&rootDSE.Get("defaultNamingContext")&">;(givenName=Kohsuke);name,description;subTree"

' Execute the query.
Set rs = oCommand1.Execute
'--------------------------------------
' Navigate the record set
'--------------------------------------
While Not rs.EOF
    WScript.Echo(rs.Fields("Name") & " , " & rs.Fields("description"))
    rs.MoveNext
 */
