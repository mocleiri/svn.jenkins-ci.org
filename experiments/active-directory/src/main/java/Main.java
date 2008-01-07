import com4j.COM4J;
import com4j.Com4jObject;
import com4j.typelibs.activeDirectory.IADs;
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
        con.open("Active Directory Provider",null,null,0);

        _Command cmd = ClassFactory.createCommand();
        cmd.activeConnection(con);

        IADs rootDSE = COM4J.getObject(IADs.class, "LDAP://RootDSE", null);

        String context = (String)rootDSE.get("defaultNamingContext");
        System.out.println("Context is "+context);

        cmd.commandText("<LDAP://"+context+">;(giveName=Kohsuke);name,description;subTree");
        _Recordset rs = cmd.execute(null, null, 0);


        while(!rs.eof()) {
            System.out.println(rs.fields().item("Name").value());
            System.out.println(rs.fields().item("Description").value());
            rs.moveNext();
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
