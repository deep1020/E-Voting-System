import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;

public class CLA {

	static int port_CTF = 3401;
	static int port_Voter = 3402;
	String serverName = "localhost";

	private ServerSocket VotersServersocket;

	static BigInteger CTF_public_key;
	static BigInteger CTF_N;
	static BigInteger Voter_public_key;
	static BigInteger Voter_N;

	RSA rsa = new RSA();
	String key_Pair = rsa.e + "," + rsa.N;

	public void getKey(String key, int number) {
		String[] keys = key.split(",");
		if (number == 1) {
			System.out.println("--Getting the CTF keys.--");
			CTF_public_key = new BigInteger(keys[0]);
			CTF_N = new BigInteger(keys[1]);
		} else {
			System.out.println("--Getting the Voter keys.--");
			Voter_public_key = new BigInteger(keys[0]);
			Voter_N = new BigInteger(keys[1]);
		}
	}

	
	
	
	static HashMap<String, BigInteger> validationNums = new HashMap<String, BigInteger>();
	private void startSender() {
		try {
			System.out.println("Connecting to CTF on port " + port_CTF);
			Socket CTF = new Socket(serverName, port_CTF);

			ObjectInputStream in = new ObjectInputStream(CTF.getInputStream());
			String msg = (String) in.readObject();
			getKey(msg, 1);
			sendKey(CTF);

			try {
				ObjectOutputStream op = new ObjectOutputStream(CTF.getOutputStream());
				op.writeObject(validationNums);
			} catch (IOException e) {
				e.printStackTrace();
			}

			CTF.close();

		} catch (IOException | ClassNotFoundException ex) {
			ex.printStackTrace();
		}
	}

	public static String readFile() {

		String filename = "./" + "VerifyNumbers.txt";
		String line = null;
		StringBuilder sb = new StringBuilder();

		try {
			FileReader f = new FileReader(filename);
			BufferedReader br = new BufferedReader(f);

			while ((line = br.readLine()) != null) {

				String[] validNumFromFile = line.split(",");

				if (validNumFromFile.length == 2) {
					validationNums.put(validNumFromFile[0], new BigInteger(validNumFromFile[1]));
				}
			}
			br.close();
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open the file '" + filename + "'");
		} catch (IOException ex) {
			System.out.println("Error reading the file '" + filename + "'");
		}

		String data = sb.toString();
		return data;
	}

	public void sendKey(Socket socket) {
		try {
			ObjectOutputStream op = new ObjectOutputStream(socket.getOutputStream());
			op.writeObject(key_Pair);
			op.flush();
		} catch (IOException e) {
			System.err.println("Error occured:" + e);
		}
	}

	
	private void startServer() {

		new Thread() {
			public void run() {
				try {
					Boolean flag = false;
					VotersServersocket = new ServerSocket(port_Voter);
					Socket voter = null;
					while (!flag) {
						System.out.println("Here Waiting for the the Voter at port no:" + port_Voter);
						voter = VotersServersocket.accept();
						System.out.println("Here Connected to Voter's Server.");

						String choice;
						ObjectInputStream ip = new ObjectInputStream(voter.getInputStream());
						choice = (String) ip.readObject();

						switch (choice.substring(0, 1)) {
						case "#":
							System.out.println("Voter wants to get a Validation number.");
							try {
								sendKey(voter);
								ObjectInputStream input = new ObjectInputStream(voter.getInputStream());
								String key = (String) input.readObject();
								getKey(key, 2);
							} catch (Exception e) {
								e.printStackTrace();
							}

							String[] str_list = choice.split(",");

							String userName = rsa.decrypt(str_list[1], Voter_public_key, Voter_N);

							ObjectOutputStream op = new ObjectOutputStream(voter.getOutputStream());

							BigInteger val = null;

							if (validationNums.containsKey(userName)) {
								val = (validationNums.get(userName));
								System.out.println("Validation Number for this candidate exists! Please continue to vote.");
							} else {
								val = new BigInteger(25, new Random());
								validationNums.put(userName, val);
								System.out.println("Validation number has been generated." + val);
								String userValidationNo = "\n" + userName + "," + val.toString();
								write_file(userValidationNo);
								startSender();
							}

							val = rsa.encrypt(val);
							op.writeObject(val);
							op.flush();
						}
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}.start();
	}

	
	public static void write_file(String data) {
		String fileName = "VerifyNumbers.txt";
		try {
			File file = new File(fileName);
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter write = new BufferedWriter(fw);
			write.write(data);
			write.newLine();
			write.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		CLA cla = new CLA();
		cla.readFile();
		cla.startServer();
		cla.startSender();
	}

}
