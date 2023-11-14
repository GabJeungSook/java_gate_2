/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package turnstilegate;

import Door.Access.Command.CommandDetail;
import Door.Access.Command.CommandParameter;
import Door.Access.Command.INCommand;
import Door.Access.Command.INCommandResult;
import Door.Access.Connector.ConnectorAllocator;
import Door.Access.Connector.ConnectorDetail;
import Door.Access.Connector.E_ControllerType;
import Door.Access.Connector.INConnectorEvent;
import Door.Access.Connector.TCPClient.TCPClientDetail;
import Door.Access.Data.AbstractTransaction;
import Door.Access.Data.BytesData;
import Door.Access.Data.INData;
import Door.Access.Door8800.Command.Data.AlarmTransaction;
import Door.Access.Door8800.Command.Data.ButtonTransaction;
import Door.Access.Door8800.Command.Data.CardTransaction;
import Door.Access.Door8800.Command.Data.DefinedTransaction;
import Door.Access.Door8800.Command.Data.Door8800WatchTransaction;
import Door.Access.Door8800.Command.Data.DoorSensorTransaction;
import Door.Access.Door8800.Command.Data.SoftwareTransaction;
import Door.Access.Door8800.Command.Data.SystemTransaction;
import Door.Access.Door8800.Command.Door.OpenDoor;
import Door.Access.Door8800.Command.Door.Parameter.OpenDoor_Parameter;
import Door.Access.Door8800.Command.System.BeginWatch;
import Door.Access.Door8800.Command.System.ReadTCPSetting;
import Door.Access.Door8800.Door8800Identity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
/**
 *
 * @author USER
 */
public class TurnstileGate implements INConnectorEvent  {
        private ConnectorAllocator _Allocator;
        private String LocalIP;
        private int LocalPort;
        private final Semaphore available = new Semaphore(0, true);
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        TurnstileGate test = new TurnstileGate();
         test.syn();
    }
    
      public TurnstileGate() {

        _Allocator = ConnectorAllocator.GetAllocator();
     
        _Allocator.AddListener(this);
        try {
            String LocalIP = "192.168.110.20";
            int LocalPort = 8000;
            _Allocator.Listen(LocalIP, LocalPort);
            readTCPSetting();
        } catch (Exception e) {
            // Handle the exception
        }
          //readTCPSetting();
    }
    
     public void syn() {
        try {
            available.acquire();
        } catch (Exception e) {
        }

    }
     
     private void beginWatch() {
        BeginWatch cmd = new BeginWatch(new CommandParameter((getCommandDetail())));
        _Allocator.AddCommand(cmd);
        //System.out.println("watching..");

    }
     
    public void readTCPSetting() {

        CommandParameter parameter = new CommandParameter(getCommandDetail());
        ReadTCPSetting cmd = new ReadTCPSetting(parameter);
        _Allocator.AddCommand(cmd);
        System.out.println("Start reading device TCP parameters");
    }
    
    public CommandDetail getCommandDetail() {
        TCPClientDetail tcpClient = new TCPClientDetail("192.168.110.20", 8000);
        tcpClient.Timeout = 5000;
        tcpClient.RestartCount = 0;	
        Door8800Identity idt = new Door8800Identity("MC-5924T23010053", "FFFFFFFF", E_ControllerType.Door8900);
        CommandDetail commandDetail = new CommandDetail();
        commandDetail.Connector = tcpClient;
        commandDetail.Identity = idt;
        return  commandDetail;
    }
    
    
    @Override
    public void CommandCompleteEvent(INCommand cmd, INCommandResult result) {
    
    }
    
       @Override
    public void CommandProcessEvent(INCommand cmd) {
    	 //System.out.println("current command:"+cmd.getClass().toString()+",Current progress:"+cmd.getProcessStep()+"/"+cmd.getProcessMax() );
   
         beginWatch();
    }
    
      @Override
    public void ConnectorErrorEvent(INCommand cmd, boolean isStop) {
        String sCmd=cmd.getClass().toString();
         if (isStop) {
          //     
            } else {
           //    
            }
        
    }
    
      @Override
    public void ConnectorErrorEvent(ConnectorDetail detial) {
        try {         
           System.out.println("Network channel failure:");
        } catch (Exception e) {
            System.out.println("doorAccessiodemo.frmMain.ConnectorErrorEvent() -- " + e.toString());
        }
    }
    
     @Override
    public void CommandTimeout(INCommand cmd) {          
       
    }
    
      @Override
    public void PasswordErrorEvent(INCommand cmd) {
      
    }
    
     @Override
    public void ChecksumErrorEvent(INCommand cmd) {
        
    }
    
       public void openDoor(int door_number) {
       CommandDetail commandDetail = getCommandDetail();
       OpenDoor_Parameter parameter = new OpenDoor_Parameter(commandDetail);
        parameter.Door.SetDoor(door_number, 1);
        OpenDoor cmd = new OpenDoor(parameter);
        _Allocator.AddCommand(cmd);
    }
       
    public void WatchEvent(ConnectorDetail detial, INData event) {
              try {
            Door8800WatchTransaction watchEvent = (Door8800WatchTransaction) event;
                  AbstractTransaction tr = (AbstractTransaction) watchEvent.EventData;
                  CardTransaction card = (CardTransaction) watchEvent.EventData;
                   boolean found = false;
                   String cardFound = "";
                   String door_scanned = "";
                   String scanned_type = "";
                   String door_name = "Door1";
                   String request_type = "checking";
                URL url = new URL("http://usmgate.org/api/check-card");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                
                if(card.DoorNum() == 2)
                {
                    door_scanned = "left";
                    scanned_type = "entry";
                }else{
                    door_scanned = "right";
                    scanned_type = "exit";
                }
                
              String parameters = "id_number="+card.CardData+"&source="+door_scanned+"&scanned_type="+scanned_type+"&door_name="+door_name+"&request_type="+request_type+"";
                
                try (OutputStream os = connection.getOutputStream()) {
                byte[] input = parameters.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                }
                int responseCode = connection.getResponseCode();
                
                  if (responseCode == HttpURLConnection.HTTP_OK) {
                       BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                       String line;
                       StringBuilder response = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    connection.disconnect();  
                    String responseData = response.toString();
                     int successIndex = responseData.indexOf("\"success\":");
                     int valueStart = responseData.indexOf(":", successIndex) + 1;
                      int valueEnd = responseData.indexOf(",", valueStart);
                        if (valueEnd == -1) {
                        valueEnd = responseData.indexOf("}", valueStart);
                        }
                         String successValue = responseData.substring(valueStart, valueEnd).trim();
                         boolean success = Boolean.parseBoolean(successValue);
                         
                         if(success)
                         {
                             System.out.println(responseData);
                             System.out.println(event);
                              CommandDetail commandDetail = getCommandDetail();
                              OpenDoor_Parameter parameter = new OpenDoor_Parameter(commandDetail); 
                              parameter.Door.SetDoor(card.DoorNum(), 1);
                              OpenDoor cmd = new OpenDoor(parameter);
                              _Allocator.AddCommand(cmd);
                              
                              //save to database
                              request_type = "saving";
                              try{
                                   URL url1 = new URL("http://usmgate.org/api/check-card"); 
                                   HttpURLConnection connection1 = (HttpURLConnection) url1.openConnection();
                                    connection1.setRequestMethod("POST");
                                    connection1.setDoOutput(true);
                                     String parameters1 = "id_number="+card.CardData+"&source="+door_scanned+"&scanned_type="+scanned_type+"&door_name="+door_name+"&request_type="+request_type+"";
                                     try (OutputStream os1 = connection1.getOutputStream()) {
                                     byte[] input1 = parameters1.getBytes(StandardCharsets.UTF_8);
                                     os1.write(input1, 0, input1.length);
                                     }
                                      int responseCode1 = connection1.getResponseCode();
                                       if (responseCode1 == HttpURLConnection.HTTP_OK) {
                                            BufferedReader reader1 = new BufferedReader(new InputStreamReader(connection1.getInputStream()));
                                            String line1;
                                            StringBuilder response1 = new StringBuilder();
                                             while ((line1 = reader1.readLine()) != null) {
                                                    response1.append(line1);
                                             }
                                              reader1.close();
                                              connection1.disconnect(); 
                                              System.out.println("saved to database");
                                       }
                              }catch(Exception e)
                              {
                                   System.out.println(e.toString());
                              }
                             
//                                 if (responseCode == HttpURLConnection.HTTP_OK) {
//                                      
//                                        System.out.println("saved to database");
//                                 }else{
//                                      System.out.println("failed saving to database");
//                                 }
                         }
                     
                            
                } else {
                     System.out.println("API Request failed with response code: " + responseCode);
                }
                
                  
//                if (responseCode == HttpURLConnection.HTTP_OK) { 
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                    String line;
//                    StringBuilder response = new StringBuilder();
////
//                    while ((line = reader.readLine()) != null) {
//                        response.append(line);
//                    }
//                    reader.close();
////
////                   
//                    String responseData = response.toString();
//                    System.out.print(responseData);
                            //open door     
//                            


//                } else {
//                    System.out.println("API Request failed with response code: " + responseCode);
//                }

              

            
            /*
            
            */
//             System.out.println("Event");
//            System.out.print(event.toString());
//            StringBuilder strBuf = new StringBuilder(100);
//            strBuf.append("Data monitoring:");
//            if (event instanceof Door.Access.Door89H.Command.Data.CardTransaction) {
//                  Door8800WatchTransaction watchEvent = (Door8800WatchTransaction) event;
//                  AbstractTransaction tr = (AbstractTransaction) watchEvent.EventData;
//                  CardTransaction card = (CardTransaction) watchEvent.EventData;
//                   if ("12339606".equals(card.CardData)) { 
//                       System.out.print(card.CardData);
//                 // Test test = new Test();
//        
//                            //test.openDoor();
//                   }
////                Door8800WatchTransaction WatchTransaction = (Door8800WatchTransaction) event;
////                strBuf.append("，SN：");
////                strBuf.append(WatchTransaction.SN);
////                strBuf.append("\n");
//            } else {
//                strBuf.append("，Unknown Event：");
//                strBuf.append(event.getClass().getName());
//            }
           // System.out.println(strBuf);
        } catch (Exception e) {
            System.out.println("doorAccessiodemo.frmMain.WatchEvent() -- " + e.toString());
        }
    }
    
       
    @Override
    public void ClientOnline(ConnectorDetail client) {
	
    }
    
     @Override
    public void ClientOffline(ConnectorDetail client) {
       
    }
       
       
    
}
