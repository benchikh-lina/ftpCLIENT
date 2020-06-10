package sample;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.swing.*;

public class Controller extends Component implements Initializable {

    public static Socket socket;
    public static Socket data;
    public static PrintStream outputStream;

    private final Image file_image = new Image("/pictures/file.png");
    private final Image document_image = new Image("pictures/documents.png");

    private Image[] listOfImages = {file_image, document_image};

    //pour le login recupere le nom quand on va sisait dans le champ username
    @FXML
    public  TextField inpName;

    //pour le login recupere le password quand on va sisait dans le champ username
    @FXML
    public   PasswordField inpPassword;

    private static  BufferedWriter  writer,writerData;

    public static long restartPoint = 0L;

    //pour les messages d'erreurs de la login interface
    @FXML
    public   Text error;

    //pour les messages d'erreur
    @FXML
    public Text text1;
 //pour les messages de cwd
    @FXML
    public Text text;

    public static BufferedReader reader;

    private static BufferedInputStream inputStr;

    //pour recuperer la list quelle est dans le fxml
    @FXML
    ListView<String> listView;

    //pour recuperer la 2eme list
    @FXML
    ListView<String> sousList;


    /*---------------------------------------------------------- login ----------------------------------------------------------------*/

    public void login(ActionEvent actionEvent) throws IOException {
        connect("127.0.0.1", 21);
        String username=inpName.getText();
        String pass=inpPassword.getText();
        if (username.isEmpty()){
            error.setText("verfier le chemp de username ");

        }else if(pass.isEmpty()){
            error.setText("verifier le champ de mot de passe");
        }else if(username.isEmpty() && pass.isEmpty()){
            error.setText("verifier le champ de username ou de mot de passe the musn't be empty");
        }else{
            String response = send("user " + username);
            if (response.startsWith("331")) {
                error.setText("Password required for "+username);
            } else if(response.startsWith("530")) {
                error.setText("Login or password incorrect!");
            }
            response = send("pass " + pass);
            if(response.startsWith("331")) {
                error.setText("password don't march with the username");
            }  else if(response.startsWith("530")) {
                error.setText("Login or password incorrect!");
            }
            else if (response.startsWith("230")){
                FXMLLoader loader = new FXMLLoader(getClass().getResource("principalePage.fxml"));
                Parent parent=loader.load();
                Scene main = new Scene(parent);
                Stage stage = (Stage)((javafx.scene.Node) actionEvent.getSource()).getScene().getWindow();
                stage.setScene(main);
                stage.show();
            }
        }

    }
    public boolean connect(String host, int port)throws UnknownHostException, IOException
    {
        //the socket that let us connect to the servet
        socket = new Socket(host, port);
        //send data to the server
        outputStream = new PrintStream(socket.getOutputStream());
        //read response from the server
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        inputStr = new BufferedInputStream(socket.getInputStream());
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        String reply;
        do {
            //read the answer of the server
            reply = reader.readLine();

            //the ansewer start with 3 number then read
        } while(!(Character.isDigit(reply.charAt(0)) &&
                Character.isDigit(reply.charAt(1)) &&
                Character.isDigit(reply.charAt(2)) &&
                reply.charAt(3) == ' '));

        //if the answer don't start with 220 so exit
        if (!reply.startsWith("220")){
            disconnect();
            return false;
        }
        //else continue
        return true;
    }

    /*-------------------------------------------------------------------- logout ----------------------------------------*/

    public  void disconnect()
    {
        if (outputStream != null) {
            try {
                logout();
                outputStream.close();
                reader.close();
                socket.close();
            } catch (IOException e) {}

            outputStream = null;
            reader = null;
            socket = null;
        }
    }

    public  boolean logout()throws IOException
    {
        //send the quit command to the server
        String response = send("quit");
        //if the answer start with 221 don't quit
        if(response.startsWith("221")) {
            return false;
        }else{
            // else exit
            return true;
        }
    }

    // if i want to quit without connecting
    public void exit(){
        Platform.exit();
    }

    /*-------------------------------- send ----------------------------------------------*/
//the methode that allow us to send the command to the server
    public static String send(String command)throws IOException
    {
        outputStream.println(command);
        String reply;
        //the answer of the server
        reply = reader.readLine();
        return (reply);
    }

    public static String getExecutionResponse(String command)throws IOException
    {
        outputStream.println(command);
        String reply;
        reply = reader.readLine();
        return reply;
    }


    public static boolean readData(String command, StringBuffer sb)throws IOException
    {
        if (!setupDataPasv(command)) return false;
        InputStream in = data.getInputStream();

        byte b[] = new byte[4096];
        int amount;
        while ((amount = in.read(b)) > 0)
        {
            sb.append(new String(b, 0, amount));
        }
        in.close();

        String reply;

        reply = reader.readLine();

        int response=Integer.parseInt(reply.substring(0, 3));

        if(response >= 200 && response < 300) return true;
        else return false;
    }

    /*-------------------------------------------------------------- pasv mode ----------------------------------------*/

    public static boolean setupDataPasv(String command) throws IOException
    {
        if (!openPasv()) return false;
        //binary mode
        outputStream.println("TYPE i");
        //the answer
        String reply;
        reply = reader.readLine();

        if (restartPoint != 0) {
            outputStream.println("rest " + restartPoint);
            restartPoint = 0;

            String reply1;
            reply1 = reader.readLine();

            Integer.parseInt(reply1.substring(0, 3));
        }
        outputStream.println(command);
        String reply2;
        reply2 = reader.readLine();

        int response2 =Integer.parseInt(reply2.substring(0, 3));

        if(response2 >= 100 && response2 < 200)
            return true;
        else return false;
    }
//le mode passive

    public static boolean openPasv() throws IOException
    {
        String tmp = getExecutionResponse("PASV");

        String ip = null;
        int port = -1;
        int debut = tmp.indexOf('(');
        int fin = tmp.indexOf(')', debut + 1);
        if (debut > 0) {
            //they must start with (
            String dataLink = tmp.substring(debut + 1, fin);
            StringTokenizer tokenizer = new StringTokenizer(dataLink, ",");

            //ip adress contains .
            ip = tokenizer.nextToken() + "." + tokenizer.nextToken() + "."
                    + tokenizer.nextToken() + "." + tokenizer.nextToken();
            //calculate the port number
            port = Integer.parseInt(tokenizer.nextToken()) * 256
                    + Integer.parseInt(tokenizer.nextToken());
            data = new Socket(ip, port);

        }
        int response=Integer.parseInt(tmp.substring(0,3));
        if(response >= 200 && response < 300) return true;
        else return false;
    }


    /*-------------------------------------------------------------------------- StOR data------------------------------------------------------------------*/

    public void stor() throws IOException {
        //clear the list
        listView.getItems().clear();
        //to show the list
        listFiles();
        //to choose the file
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogTitle("upload");
        jfc.showOpenDialog(null);
        File file = jfc.getSelectedFile();
        String filename = file.getName();
        if (filename != null) {
            storeData(file);
        }
        //to add the new file to the list with size
        listView.getItems().addAll("*" +filename +"\t\t"+sizeSTOR(filename));
        listFiles();
    }
    //pour la 2eme list
    public void SouSstor() throws IOException {
        sousList.getItems().clear();
        SouslistFiles();
        JFileChooser jfc = new JFileChooser();

        jfc.setDialogTitle("upload");
        jfc.showOpenDialog(null);
        File file = jfc.getSelectedFile();
        String filename = file.getName();
        if (filename != null) {
            storeData(file);
        }
        sousList.getItems().addAll("*" +filename +"\t\t"+sizeSTOR(filename));
        SouslistFiles();
    }
    //call the first list if the current directory is /
    //else store in the second list
    public void store() throws IOException {
        if (pwd().startsWith("257 \"/\" is current directory.")){
            stor();
        }else{
            SouSstor();
        }

    }


    public static  void storeData(File file) throws IOException
    {
        //ascii type
        send("TYPE ASCII");
        openPasv();
        writerData = new BufferedWriter(new OutputStreamWriter(data.getOutputStream()));
        inputStr = new BufferedInputStream(data.getInputStream());

        //la commande stor
        send("STOR "+file.getName());
        //the size
        byte[] bytes = new byte[16 * 1024];

        //read
        InputStream in = new FileInputStream(file);
        //send
        OutputStream outputStream = data.getOutputStream();

        int size;
        //
        while ((size = in.read(bytes)) > 0)
        {
            outputStream.write(bytes, 0, size);
        }

        data.close();
        outputStream.close();
    }

    /*------------------------------------------------------------- create new folder --------------------------------------------------------*/
    public  void newFolders() throws IOException {

        String input = JOptionPane.showInputDialog("new folder");
        //to create new filder send XMKD command
        String re= send("XMKD "+input);
        //if the folder name already exists
        if (re.startsWith("550")){
            text1.setText("directory already exists");
        }
    }
    public void newFold() throws IOException {
        //add the new folder in the sirst list if the current directory is /
        //else add it to the 2nd list
        if (pwd().startsWith("257 \"/\" is current directory.")){
            newFolders();
            listView.getItems().clear();
            listFiles();

        }else{
            newFolders();
            sousList.getItems().clear();
            SouslistFiles();
        }

    }
    /*------------------------------------------------------------- create new file ----------------------------------------------------------*/


    public  void newFile() throws IOException {
        text1.setText("");
        String input=JOptionPane.showInputDialog("");
        openPasv();
        writerData = new BufferedWriter(new OutputStreamWriter(data.getOutputStream()));
        inputStr = new BufferedInputStream(data.getInputStream());
        String re=send("STOR " + input);

        if (re.startsWith("550")){
            text1.setText("files already exist");
        }
        listView.getItems().clear();
        listFiles();
        data.close();
        inputStr.close();
        writerData.close();

    }
    public  void newSousFile() throws IOException {
        text1.setText("");
        String input=JOptionPane.showInputDialog("");
        openPasv();
        writerData = new BufferedWriter(new OutputStreamWriter(data.getOutputStream()));
        inputStr = new BufferedInputStream(data.getInputStream());
        String re=send("STOR " + input);
        if (re.startsWith("550")){
            text1.setText("files already exist");
        }
        sousList.getItems().clear();
        SouslistFiles();
        data.close();
        inputStr.close();
        writerData.close();

    }
    //new file to create the file in the first list if the current directory is /
    //else i'll create it in the second list
    public void newFiles() throws IOException {
        if (pwd().startsWith("257 \"/\" is current directory.")){
            newFile();
        }else{
            newSousFile();
        }
    }


    /*---------------------------------------------------------------------------------- Retreive date ---------------------------------------*/
//to retreive file i use the RETR command
    public  void  retreiveFile( String file) throws IOException {
        text1.setText("");
        send("TYPE ASCII");
        openPasv();
        writerData = new BufferedWriter(new OutputStreamWriter(data.getOutputStream()));
        inputStr = new BufferedInputStream(data.getInputStream());

        String re=send("RETR "+file);

        RandomAccessFile outfile = new RandomAccessFile(file, "rw");
        // On lance un restart si d�sir�
        if (restartPoint != 0) {
            outfile.seek(restartPoint);
        }
        FileOutputStream out = new FileOutputStream(outfile.getFD());
        InputStream inputStream = data.getInputStream();

        byte[] bytes = new byte[16 * 1024];

        int size;
        while ((size = inputStream.read(bytes)) > 0)
        {
            out.write(bytes, 0, size);
        }
        data.close();
        inputStream.close();
        out.close();

    }
    public void retreive(String filename) throws IOException {
        retreiveFile(filename);
        sizeRET(filename);
    }
    /*------------------------------- size --------------------------------------------------------*/
    //for the size of the uploaded file
    public   static  String  sizeSTOR(String fileName) throws IOException {
        outputStream.println("SIZE "+fileName);
        String reply;
        do {
            reply = reader.readLine();
        } while(!(Character.isDigit(reply.charAt(0)) &&
                Character.isDigit(reply.charAt(1)) &&
                Character.isDigit(reply.charAt(2)) &&
                reply.charAt(3) == ' '));

        return (reply.substring(4));

    }
    //for the size of the retreived file
    public  String  sizeRET(String fileName) throws IOException {
        outputStream.println("SIZE "+fileName);
        String reply;
        do {
            reply = reader.readLine();
        } while(!(Character.isDigit(reply.charAt(0)) &&
                Character.isDigit(reply.charAt(1)) &&
                Character.isDigit(reply.charAt(2)) &&
                reply.charAt(3) == ' '));

        String reply1;
        do {
            reply1 = reader.readLine();
        } while(!(Character.isDigit(reply1.charAt(0)) &&
                Character.isDigit(reply1.charAt(1)) &&
                Character.isDigit(reply1.charAt(2)) &&
                reply1.charAt(3) == ' '));
        if (reply.startsWith("226")){
            text1.setText(fileName +" Successfully transfered "+" size "+reply1.substring(4));
        }
        return (reply1.substring(4));

    }


    /*---------------------------------------------------- list ----------------------------------------*/
    public  void LIST() throws IOException {
        //if i am in the the /current directory i call calllist
        //else i will call callSousList
        text1.setText("");
        if (pwd().startsWith("257 \"/\" is current directory.")){
            callList();
            listView.getItems().clear();
            listFiles();
        }else{
            callSousList();
            sousList.getItems().clear();
            SouslistFiles();
        }


    }
    //pour l'affichage du context menu in the 1st list
    public void callList(){
        ContextMenu contextMenu = new ContextMenu();
        MenuItem openItem = new MenuItem();
        MenuItem deleteItem = new MenuItem();
        MenuItem downloadItem = new MenuItem();
        MenuItem renameItem = new MenuItem();


        openItem.setText("open");
        deleteItem.setText("delete");
        downloadItem.setText("download");
        renameItem.setText("rename");

        Image openIcon = new Image(getClass().getResourceAsStream("/pictures/open.png"));
        ImageView openView = new ImageView(openIcon);
        openView.setFitWidth(20);
        openView.setFitHeight(20);
        openItem.setGraphic(openView);
        Image openIcon1 = new Image(getClass().getResourceAsStream("/pictures/delete.png"));
        ImageView openView1 = new ImageView(openIcon1);
        openView1.setFitWidth(20);
        openView1.setFitHeight(20);
        deleteItem.setGraphic(openView1);
        Image openIcon2 = new Image(getClass().getResourceAsStream("/pictures/folder.png"));
        ImageView openView2 = new ImageView(openIcon2);
        openView2.setFitWidth(20);
        openView2.setFitHeight(20);
        downloadItem.setGraphic(openView2);
        Image openIcon3 = new Image(getClass().getResourceAsStream("/pictures/edit.png"));
        ImageView openView3 = new ImageView(openIcon3);
        openView3.setFitWidth(20);
        openView3.setFitHeight(20);
        renameItem.setGraphic(openView3);

// to change the directory so i use the CWD wommand
        openItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {

                String cwd = listView.getSelectionModel().getSelectedItem();
                if (cwd.contains("\t\t")) {
                    contextMenu.hide();

                } else {
                        if(cwd.startsWith("  ") ){
                            int index1 = cwd.indexOf("\t");
                                cwd = cwd.substring(2,index1);
                                try {
                                cwd(cwd);
                                callSousList();

                            } catch (IOException e) {
                                    e.printStackTrace();

                                }                            }



                }
            }
        });
        //to delete file or directory
        deleteItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {

                String cwd = listView.getSelectionModel().getSelectedItem();
                if (cwd.startsWith("  ")) {
                    int index1 = cwd.indexOf("\t");

                        cwd = cwd.substring(2,index1);
                        try {
                            directories(cwd);
                            listView.getItems().clear();
                            listFiles();
                            contextMenu.hide();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                    }
                } else if (cwd.startsWith("*")){
                    int index1 = cwd.indexOf("\t\t");
                        cwd = cwd.substring(1,index1);
                 try {
                            file(cwd);
                            listView.getItems().clear();
                            listFiles();
                            contextMenu.hide();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                }
            }
        });
        //to download a file from the server RETR
        downloadItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {

                String cwd = listView.getSelectionModel().getSelectedItem();
                if (cwd.startsWith("  ")) {
                    contextMenu.hide();
                } else if (cwd.startsWith("*")){
                    int index1 = cwd.indexOf("\t\t");

                        cwd = cwd.substring(1,index1);
                        try {
                            contextMenu.hide();
                            retreive(cwd);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                }
            }
        });
        //to rename a file or folder

        renameItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                contextMenu.hide();
                String input=JOptionPane.showInputDialog("rename : you must enter the extension");

                String cwd = listView.getSelectionModel().getSelectedItem();
                if (cwd.startsWith("  ")) {
                    int index1 = cwd.indexOf("\t");

                        cwd = cwd.substring(2,index1);
                        try {
                            //the old name
                            send("RNFR "+cwd);
                            //the new name
                            send("RNTO "+input);
                            listView.getItems().clear();
                            listFiles();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                } else if (cwd.startsWith("*")){
                    int index1 = cwd.indexOf("\t\t");
                    try {
                        cwd = cwd.substring(1,index1);

                        //the old name
                        send("RNFR "+cwd);
                        //the new name
                        send("RNTO "+input);
                        listView.getItems().clear();
                        listFiles();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        contextMenu.getItems().add(openItem);
        contextMenu.getItems().add(deleteItem);
        contextMenu.getItems().add(downloadItem);
        contextMenu.getItems().add(renameItem);


        EventHandler<WindowEvent> event= new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {

            }
        };

        contextMenu.setOnShowing(event);
        contextMenu.setOnHiding(event);
        listView.setContextMenu(contextMenu);


    }
    public void callSousList(){
        ContextMenu contextMenu = new ContextMenu();

        MenuItem openItem = new MenuItem();
        MenuItem deleteItem = new MenuItem();
        MenuItem downloadItem = new MenuItem();
        MenuItem renameItem = new MenuItem();

        openItem.setText("open");
        deleteItem.setText("delete");
        downloadItem.setText("download");
        renameItem.setText("rename");

        Image openIcon = new Image(getClass().getResourceAsStream("/pictures/open.png"));
        ImageView openView = new ImageView(openIcon);
        openView.setFitWidth(20);
        openView.setFitHeight(20);
        openItem.setGraphic(openView);
        Image openIcon1 = new Image(getClass().getResourceAsStream("/pictures/delete.png"));
        ImageView openView1 = new ImageView(openIcon1);
        openView1.setFitWidth(20);
        openView1.setFitHeight(20);
        deleteItem.setGraphic(openView1);
        Image openIcon2 = new Image(getClass().getResourceAsStream("/pictures/folder.png"));
        ImageView openView2 = new ImageView(openIcon2);
        openView2.setFitWidth(20);
        openView2.setFitHeight(20);
        downloadItem.setGraphic(openView2);
        Image openIcon3 = new Image(getClass().getResourceAsStream("/pictures/edit.png"));
        ImageView openView3 = new ImageView(openIcon3);
        openView3.setFitWidth(20);
        openView3.setFitHeight(20);
        renameItem.setGraphic(openView3);

        // to change the current directory
        openItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {

                String cwd = sousList.getSelectionModel().getSelectedItem();
                if (cwd.startsWith("  ")) {
                    int index1 = cwd.indexOf("\t");

                        cwd = cwd.substring(2,index1);
                        try {
                            cwd(cwd);
                            callSousList();
                            contextMenu.hide();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                 else {

                    contextMenu.hide();
                }
                }

        });
//to delete file or folder
        deleteItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {

                String cwd = sousList.getSelectionModel().getSelectedItem();
                if (cwd.startsWith("  ")) {
                    int index1 = cwd.indexOf("\t");
                        cwd = cwd.substring(2,index1);
                        try {
                            directories(cwd);
                            sousList.getItems().clear();
                            SouslistFiles();
                            contextMenu.hide();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                    }
                }
                if(cwd.startsWith("*")) {
                    int index1 = cwd.indexOf("\t\t");

                        cwd = cwd.substring(1,index1);

                    try {
                            file(cwd);
                            sousList.getItems().clear();
                            SouslistFiles();
                            contextMenu.hide();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            }
        });
        //to retre file from the server
        downloadItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {

                String cwd = sousList.getSelectionModel().getSelectedItem();
                if (cwd.startsWith("  ")) {
                    contextMenu.hide();
                } else if (cwd.startsWith("*")) {
                    int index1 = cwd.indexOf("\t");
                        cwd = cwd.substring(1,index1);
                        try { contextMenu.hide();
                            retreiveFile(cwd);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }
            }
        });
        //to rename i file or directory
        renameItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                contextMenu.hide();
                String input=JOptionPane.showInputDialog("rename : you must enter the extension");


                String cwd = sousList.getSelectionModel().getSelectedItem();
                if (cwd.startsWith("  ")) {
                    int index1 = cwd.indexOf("\t");

                        cwd = cwd.substring(2,index1);
                        try {
                            //the old name
                            send("RNFR "+cwd);
                            //the new name
                            send("RNTO "+input);
                            sousList.getItems().clear();
                            SouslistFiles();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                } else if (cwd.startsWith("*")){

                    int index = cwd.indexOf("\t");
                    try {
                        cwd = cwd.substring(1,index);

                        //the old name
                        send("RNFR "+cwd);
                        //the new name
                        send("RNTO "+input);
                        sousList.getItems().clear();
                        SouslistFiles();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        contextMenu.getItems().add(openItem);
        contextMenu.getItems().add(deleteItem);
        contextMenu.getItems().add(downloadItem);
        contextMenu.getItems().add(renameItem);


        EventHandler<WindowEvent> event= new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {

            }
        };
        contextMenu.setOnShowing(event);
        contextMenu.setOnHiding(event);
        sousList.setContextMenu(contextMenu);
    }

    private  String listFiles()
            throws IOException
    {
        return listFiles("");
    }

    private  String listFiles(String params) throws IOException
    {
        StringBuffer files = new StringBuffer();
        StringBuffer dirs = new StringBuffer();
        if (!getAndParseDirList(params, files,dirs))
        {
            System.out.println("Error getting file list");
        }
        return files.toString();
    }

    private  boolean getAndParseDirList(String params, StringBuffer files  ,StringBuffer dirs)throws IOException
    {
        files.setLength(0);
        dirs.setLength(0);

//command namelist
        String shortList = processFileListCommand("NLST " + params);
        //list command
        String longList = processFileListCommand("LIST " + params);

        // On coupe les lignes
        StringTokenizer sList = new StringTokenizer(shortList, "\n");
        StringTokenizer lList = new StringTokenizer(longList, "\n");

        // A not� que les deux lists ont le m�me nombre de ligne.
        while ((sList.hasMoreTokens()) && (lList.hasMoreTokens())) {
            String sString = sList.nextToken();
            String lString = lList.nextToken();

            if (lString.length() > 0) {
                if (lString.startsWith("d")) {
                    String no="  " + sString.trim()+"\t" ;
                    listView.getItems().addAll(no);
                } else if (lString.startsWith("-")) {
                    String no="*"+ sString.trim() + "\t\t                 " + sizeSTOR(sString);
                    listView.getItems().addAll(no);
                }


                listView.setCellFactory(param -> new ListCell<String>() {
                    private ImageView imageView = new ImageView();

                    @Override
                    public void updateItem(String name, boolean empty) {
                        super.updateItem(name, empty);
                        if (empty) {
                            setText(null);
                            setGraphic(null);
                        }  else if (name.contains("\t\t")) {
                            imageView.setImage(listOfImages[1]);
                            imageView.setFitHeight(40);
                            imageView.setFitWidth(40);
                            setText(name);
                            setGraphic(imageView);
                        }else {
                            imageView.setImage(listOfImages[0]);
                            imageView.setFitHeight(40);
                            imageView.setFitWidth(40);
                            setText(name);
                            setGraphic(imageView);
                        }
                    }

                });
            }

        }

        if (files.length() > 0)  {  files.setLength(files.length() - "\n".length());  }
        if (dirs.length() > 0)  {  dirs.setLength(dirs.length() - "\n".length());  }

        return true;
    }


    /*----------- sous list -------------*/

    private  String SouslistFiles()
            throws IOException
    {
        return SousListFiles("");
    }

    private  String SousListFiles(String params) throws IOException
    {
        StringBuffer files = new StringBuffer();
        StringBuffer dirs = new StringBuffer();
        if (!SousList(params, files,dirs))
        {
            System.out.println("Error getting file list");
        }
        return files.toString();
    }


    private  boolean SousList(String params, StringBuffer files , StringBuffer dirs)throws IOException
    {
        files.setLength(0);
        dirs.setLength(0);

        String shortList = processFileListCommand("NLST " + params);
        String longList = processFileListCommand("LIST " + params);

        StringTokenizer sList = new StringTokenizer(shortList, "\n");
        StringTokenizer lList = new StringTokenizer(longList, "\n");


        while ((sList.hasMoreTokens()) && (lList.hasMoreTokens())) {
            String sString = sList.nextToken();
            String lString = lList.nextToken();
            if (lString.length() > 0) {
                if (lString.startsWith("d")) {
                    String no="  " + sString.trim() +"\t";
                    sousList.getItems().addAll(no);
                } else if (lString.startsWith("-")) {
                    String no="*"+ sString.trim() + "\t\t                 " + sizeSTOR(sString);
                    sousList.getItems().addAll(no);
                }


                sousList.setCellFactory(param -> new ListCell<String>() {
                    private ImageView imageView = new ImageView();

                    @Override
                    public void updateItem(String name, boolean empty) {
                        super.updateItem(name, empty);
                        if (empty) {
                            setText(null);
                            setGraphic(null);
                        }  else if (name.contains("\t\t")) {
                            imageView.setImage(listOfImages[1]);
                            imageView.setFitHeight(40);
                            imageView.setFitWidth(40);
                            setText(name);
                            setGraphic(imageView);
                        }else if(name.contains("\t")){
                            imageView.setImage(listOfImages[0]);
                            imageView.setFitHeight(40);
                            imageView.setFitWidth(40);
                            setText(name);
                            setGraphic(imageView);
                        }
                    }
                });
            }
        }
        if (files.length() > 0)  {  files.setLength(files.length() - "\n".length());  }
        if (dirs.length() > 0)  {  dirs.setLength(dirs.length() - "\n".length());  }

        return true;
    }

    public static String processFileListCommand(String command)throws IOException
    {
        StringBuffer reply = new StringBuffer();
        String replyString;
        boolean success = readData(command, reply);
        if (!success)
        {
            return "";
        }
        replyString = reply.toString();
        if(reply.length() > 0)
        {
            return replyString.substring(0, reply.length() - 1);
        }
        else
        {
            return replyString;
        }
    }


    /*----------------------------------------- pwd ---------------------------------*/
    public  String pwd()throws IOException
    {
        text.setText("");
        String response = getExecutionResponse("pwd");
        int in=response.indexOf("is");
        text.setText(response.substring(4,in));
        return response;
    }

    /*---------------------------- cwd------------------*/

    public String cwd(String filename) throws IOException {
        text.setText("");
        outputStream.println("cwd "+filename);
        String reply;
        do {
            reply = reader.readLine();
        } while(!(Character.isDigit(reply.charAt(0)) &&
                Character.isDigit(reply.charAt(1)) &&
                Character.isDigit(reply.charAt(2)) &&
                reply.charAt(3) == ' '));


        if (reply.startsWith("550")) System.out.println(" isn't a directory ");
        else{
            sousList.getItems().clear();
            sousList.getItems().addAll(SouslistFiles());
            listView.getItems().clear();
            listView.getItems().addAll(filename);
        }
        int in=reply.indexOf("is");
        int indexOf=reply.indexOf("successful.");
        int indexOf1=reply.indexOf("successful.");
       // String in=reply.substring(14);
        text.setText(reply.substring(19,in));
        return (reply);

    }
    /*--------------------------------- delete file --------------------------------------*/
    public void file(String filename) throws IOException {
        send("DELE "+filename);
    }

    /*--------------------------------- delete directory ---------------------------*/
    public void directories(String filename) throws IOException, InterruptedException {
        send("RMD "+filename);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }
    /*----------------------------------- parent directory -----------------------------*/

    public void back(ActionEvent actionEvent) throws IOException {
        text.setText("");
        send("CDUP");
        sousList.getItems().clear();
        listView.getItems().clear();
        listFiles();

    }
}