package bot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class FacebookBotFinalServlet extends HttpServlet {
	private String token="InsertYourTokenHere";
	private String TargetUrl="https://graph.facebook.com/v2.6/me/messages?access_token="+token;
	
	/* Facebook per verifare il webhook invia una richiesta GET del tipo 
	 * https://%WebHookUrl%/?hub.verify_token=verifycode&hub.challenge=somefbgeneratedcode
	 * e si aspetta in risposta hub.challenge.
	 */
	public void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException{
			String queryString = req.getQueryString();
			String msg = "";
			if (queryString != null) {
				String verifyToken = req.getParameter("hub.verify_token");
				String challenge = req.getParameter("hub.challenge");
				msg = challenge;
			
	           if(verifyToken.equals("verify")){
			res.getWriter().write(msg);
			res.getWriter().flush();
			res.getWriter().close();
			res.setStatus(HttpServletResponse.SC_OK);
			return;
	           }
			}
		}
	/* Quando qualcuno manda un messaggio di testo al Bot,facebook apre un OutputStream verso il nostro webhook
	 * inviando un flusso di byte,che equivale ad una String formattata in JSON del tipo
	 * 
	 * {object:page,entry:[{id:967683980042454,time:1480516467930,messaging:[{sender:{id:1212697882118340},
     * recipient:{id:967683980042454},timestamp:1480516467899,message:{mid:mid.1480516467899:d81e516520,seq:2042,text:%testo%}}]}]}	 
     * 
     * A questo punto per poter mandare una risposta,il webhook effettua una chiamata POST a 
     * https://graph.facebook.com/v2.6/me/messages?access_token="+token che a sua volta invierà un  
     * segnale di Acknowledge-->  {object:page,entry:[{id:967683980042454,time:1480516468622,messaging:[{sender:{id:1212697882118340},
	 * recipient:{id:967683980042454},timestamp:0,delivery:{mids:[mid.1480516468240:ae20c6a591],watermark:1480516468240,seq:2044}}]}]}
	 * 
	 * che noi dovremo ignorare per non mandare in loop il programma
	 */
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		 String JSON=StreamToString(req.getInputStream(), 1000).replaceAll("\"", "");  //Catturo l'input Stream e lo converto in String
		 URL url = new URL(getTargetUrl());
		 HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		 //Se non setto il Timeout in caso di errori vari ci mette troppo tempo ad arrestarsi
		 connection.setConnectTimeout(5000);//5 secs
		 connection.setReadTimeout(5000);//5 secs 
		 connection.setRequestMethod("POST"); //La SendMessage API di facebook accetta solo richieste POST
		 connection.setDoOutput(true); // Abilito la scrittura in OutPut
		 connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");  //Setto l'header del messaggio,nel mio caso json
		 OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(),"UTF-8");  
		 if(JSON.contains("message")){  //Condizione necessaria,altrimenti il bot entra in un ciclo infinito per colpa degli ack
		 String finalText=postAPI(getText(JSON));
		 out.write("{\"recipient\":{\"id\":\""+getSender(JSON)+"\"},\"message\":{\"text\":\""+finalText+"\"}}");
		 out.flush();  //flush "pulisce" il buffer garantendo che tutti i byte vengano inviati
		 out.close();   //Bisogna sempre chiudere gli OutputWriter,altrimenti non funziona niente
		 int res = connection.getResponseCode();
		 System.out.println(res); //Stampa a schermo lo status code della chiamata.se il bot funziona ad ogni chiamata restituisce 200 
		 }
		 connection.disconnect();
		
	}
	public String StreamToString(final InputStream is, final int bufferSize) {
//Trasforma il flusso di byte proveniente dallo Stream in Stringa
	    final char[] buffer = new char[bufferSize];
	    final StringBuilder out = new StringBuilder();
	    try (Reader in = new InputStreamReader(is, "UTF-8")) {
	        for (;;) {
	            int rsz = in.read(buffer, 0, buffer.length);
	            if (rsz < 0)
	                break;
	            out.append(buffer, 0, rsz);
	        }
	    }
	    catch (UnsupportedEncodingException ex) {
	        /* ... */
	    }
	    catch (IOException ex) {
	        /* ... */
	    }
	    return out.toString();
	}
	public String getTargetUrl(){
	    	return TargetUrl;
	    }
	 public String postAPI(String text) throws MalformedURLException,IOException{
		 URL url = new URL("https://api.api.ai/v1/query?v=20150910");
		 HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		 connection.setConnectTimeout(5000);//5 secs
		 connection.setReadTimeout(5000);//5 secs 
		 connection.setRequestMethod("POST"); //
		 connection.setDoOutput(true); // 
		 connection.setRequestProperty("Content-Type", "application/json;charset=utf-8"); 
		 connection.setRequestProperty("Authorization", "Bearer c471e7d264be4f27a40225663eec7b8a"); //Aut richiesta da api.ai
		 OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(),"UTF-8");  
		 out.write("{\"query\":[\""+text+"\"],\"lang\":\"it\",\"sessionId\":\"1234567890\"}");
		 out.flush(); 
		 out.close();   
		 connection.getResponseCode();
		 String ProcessedText=getResposeText(StreamToString(connection.getInputStream(), 2000));
		 connection.disconnect();
		 return ProcessedText;
		 
	}
	public String getResposeText(String s){
		 String [] tmp=s.split("\"speech\": \"");
		 String text="";
		 try{
		 text=tmp[1].substring(0, tmp[1].indexOf("\","));
		 }catch (Exception e){
			  text=tmp[1].substring(0, tmp[1].indexOf("\""));
		 }
		 return text;
	}
    public String getText(String JSON){
	    	String [] tmp=JSON.split(",text:");
	    	String text=tmp[1].substring(0, tmp[1].indexOf("}}"));
	    	
	    	return text;
	    }
	 public String getSender(String JSON){
	    	String [] tmp=JSON.split(":");
	    	String Sender=tmp[7].substring(0, 16);
	    	return Sender;
	    }
}
