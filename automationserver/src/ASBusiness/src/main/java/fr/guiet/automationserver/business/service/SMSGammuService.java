package fr.guiet.automationserver.business.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import fr.guiet.automationserver.business.helper.DateUtils;
import fr.guiet.automationserver.dto.SMSDto;

/**
 * Helper class to send SMS via Gammu software
 * 
 * @author guiet
 *
 *
 */
public class SMSGammuService implements Runnable, IMqttable {

	private static Logger _logger = LogManager.getLogger(SMSGammuService.class);
	private String[] _configRecipientList = null;
	private String _configGammu = null;
	private HashMap<String, Date> _messageMap = new HashMap<String, Date>();
	private Timer _timer = null;
	private boolean _isStopped = false; // Service arrete?
	private ConcurrentLinkedQueue<SMSDto> _queue = new ConcurrentLinkedQueue<SMSDto>();
	private boolean _smsSendingInProgress = false;
	private static String MQTT_TOPIC_SMS_SERVICE = "guiet/automationserver/smsservice";
	private ArrayList<String> _mqttTopics = new ArrayList<String>();
	private List<String> _safeSms = null;

	public SMSGammuService() {

		InputStream is = null;
		try {			
			
			String configPath = System.getProperty("automationserver.config.path");
			is = new FileInputStream(configPath);

			Properties prop = new Properties();
			prop.load(is);

			String[] permanentSmsArray = prop.getProperty("sms.safe.guid").split(",");
										
			_safeSms = Arrays.<String>asList(permanentSmsArray);
			
			_configRecipientList = prop.getProperty("sms.recipients").split(",");
			_configGammu = prop.getProperty("gammu.config");

			CreateSmsFloodproofTask();

			_mqttTopics.add(MQTT_TOPIC_SMS_SERVICE);

		} catch (FileNotFoundException e) {
			_logger.error(
					"Impossible de trouver le fichier de configuration classpath_folder/config/automationserver.properties",
					e);
		} catch (IOException e) {
			_logger.error(
					"Erreur lors de la lecture du fichier de configuration classpath_folder/config/automationserver.properties",
					e);
		}
	}

	/**
	 * Send SMS using gammu software
	 * 
	 * @param sms
	 * @param useRecipientInConfig
	 */
	public void sendMessage(SMSDto sms) {

		// Enqueue SMS to send
		_queue.add(sms);

	}

	// Création de la tache de sauvegarde en bdd
	private void CreateSmsFloodproofTask() {

		_logger.info("Creating sms flood proof task");

		TimerTask floodProofTask = new TimerTask() {
			@Override
			public void run() {

				try {

					Date currentDate = new Date();
					Iterator<Entry<String, Date>> entryIt = _messageMap.entrySet().iterator();

					// Iterate over all the elements
					while (entryIt.hasNext()) {
						Entry<String, Date> entry = entryIt.next();

						Date lastMessageDate = entry.getValue();

						Long elapsedTime = DateUtils.minutesBetweenDate(lastMessageDate, currentDate);
						// _logger.info("Flood Task, processing mess : " + entry.getKey().toString() +
						// ", elapsedtime : " + elapsedTime);

						// Purge messages older than 10 minutes
						if (elapsedTime > 10) {
							_logger.info("Removing SMS message UUID from cache : " + entry.getKey().toString());
							entryIt.remove();
						}

					}
				} catch (Exception e) {
					_logger.error("Error occured in sms flood proof task", e);
				}
			}
		};

		_timer = new Timer(true);
		// Remove message every ten minutes...
		_timer.schedule(floodProofTask, 5000, 600000);

		_logger.info("Sms flood proof task created");
	}

	private boolean CheckSmsFlooding(SMSDto smsDto) {

		String messId = smsDto.getId();

		if (!_messageMap.containsKey(messId)) {
			
			if (_safeSms.contains(messId)) {
				_logger.info("SMS message with UUID : " + messId + " will be send as it is on the safe list");
				return false;
			}
			
			_logger.info("Adding SMS message UUID to cache : " + messId.toString());
			_messageMap.put(messId, new Date());
			return false;
		} else {
			// Update date
			_messageMap.put(messId, new Date());

			// Message already in HashMap and has not been removed yet
			// so flooding detected
			return true;
		}

	}

	private CommandLine makeCommand(SMSDto sms) {

		// TODO : Add gammu config file
		CommandLine commandLine = new CommandLine("gammu");
		commandLine.addArgument("-c");
		commandLine.addArgument(_configGammu);
		commandLine.addArgument("sendsms");
		commandLine.addArgument("TEXT");
		commandLine.addArgument(sms.getRecipient());
		commandLine.addArgument("-text");
		commandLine.addArgument(sms.getMessage());
		return commandLine;
	}

	private void SendSMS(SMSDto sms) {

		_smsSendingInProgress = true;

		MyResultHandler drh = new MyResultHandler(sms);

		// String cmd = "gammu -c configfile -TEXT -texts";

		CommandLine cmdLine = makeCommand(sms);

		// Timeout set to 30s
		ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);

		DefaultExecutor de = new DefaultExecutor();

		// Define successful exit value
		de.setExitValue(0);

		de.setWatchdog(watchdog);

		// Start asynchronous thread
		try {
			de.execute(cmdLine, drh);
		} catch (IOException e) {
			_logger.error("Error while sending SMS", e);
		}

	}

	private class MyResultHandler extends DefaultExecuteResultHandler {

		private SMSDto _sms = null;

		public MyResultHandler(SMSDto sms) {
			_sms = sms;
		}

		@Override
		public void onProcessComplete(final int exitValue) {
			super.onProcessComplete(exitValue);

			_smsSendingInProgress = false;

			_logger.info(String.format("SMS sent successfully ! Recipient : %s; Message : %s", _sms.getRecipient(),
					_sms.getMessage()));
		}

		@Override
		public void onProcessFailed(final ExecuteException e) {
			super.onProcessFailed(e);

			_smsSendingInProgress = false;

			String mess = String.format("Error occured while sending SMS ! Recipient : %s; Message : %s",
					_sms.getRecipient(), _sms.getMessage());
			_logger.error(mess, e);

			// Send mail then!!
			MailService mailService = new MailService();
			mailService.SendMailSSL("AutomationServer - Error sending SMS", mess);
		}
	}

	/**
	 * Stops water heater properly
	 */
	public void StopService() {

		// TODO : Create base service class
		_logger.info("Stopping SMS Gammu service...");

		_isStopped = true;
	}

	@Override
	public void run() {

		_logger.info("Starting SMS Gammu service...");

		while (!_isStopped) {

			try {

				// No sms sending in progress?
				if (!_smsSendingInProgress) {

					// Check if sms need to be send!
					SMSDto sms = _queue.poll();

					// Sms to send?
					if (sms != null) {

						// message already sent one time?
						if (CheckSmsFlooding(sms))
							continue;

						// if (useRecipientInConfig) {
						for (String recipient : _configRecipientList) {
							sms.setRecipient(recipient);
							SendSMS(sms);
						}
						// } else {
						// Not useful in my case but...
						// SendSMS(sms);
						// }
					}
				}

				// Every 1s
				Thread.sleep(1000);

			} catch (Exception e) {
				_logger.error("Error occured in SMS Gammu service", e);
			}
		}

	}

	@Override
	public boolean ProcessMqttMessage(String topic, String message) {
		
		boolean messageProcessed = false;

		if (topic.equals(MQTT_TOPIC_SMS_SERVICE)) {

			messageProcessed = true;
			
			JSONObject json = new JSONObject(message);
			String messageId = json.getString("messageid");
			String messageTxt = json.getString("messagetext");
			
			SMSDto sms = new SMSDto(messageId);
			sms.setMessage(messageTxt);
			
			sendMessage(sms);
		}

		return messageProcessed;
	}

	@Override
	public ArrayList<String> getTopics() {
		return _mqttTopics;
	}

}
