import java.net.*;
import java.io.*;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

public class Voter {
	
	// Voter will send a message to CLA asking for validation number and CLA will return a random validation number to the user. 
	// After recieving a valdidation number, the voter sends their vote and validation number to CTF

	String un, pwd;
	BigInteger verify_num;
	
	static int CLAport =  3402;
	static int CTFport = 3403;

	private Socket CTF_socket;
	private Socket CLA_socket;
	
	static BigInteger CTF_public_key;
	static BigInteger CTF_num;
	static BigInteger CLA_public_key;
	static BigInteger CLA_num;

	String server_name = "localhost";
	
	static RSA rsa = new RSA();
	static String public_key = rsa.e + "," + rsa.N;

	
	public void getKey(String key, String server_name) {
		String[] keyList = key.split(",");
		if (server_name.equals("CLA") ) {
			CLA_public_key = new BigInteger(keyList[0]);
			CLA_num = new BigInteger(keyList[1]);
		} else {
			CTF_public_key = new BigInteger(keyList[0]);
			CTF_num = new BigInteger(keyList[1]);
		}
	}

	public void verifyNumber() throws ClassNotFoundException{
		BigInteger value = null;
		
		try {
			System.out.println("Connection to CLA at port in process " + CLAport);
			CLA_socket = new Socket(server_name, CLAport);
			System.out.println("Connected to CLA");
			String encryptUsername = rsa.encrypt(un);
			
			// creating a new object.
			ObjectOutputStream op = new ObjectOutputStream(CLA_socket.getOutputStream());
			// writing into the object.
			op.writeObject("#," + encryptUsername);
			// flush to make the buffer empty for further data to store.
			op.flush();
			
			// creating object input stream for object we create before
			ObjectInputStream ip = new ObjectInputStream(CLA_socket.getInputStream());
			// read and print what we wrote
			String CLAkey = (String) ip.readObject();

			getKey(CLAkey, "CLA");

			ObjectOutputStream out_key = new ObjectOutputStream(CLA_socket.getOutputStream());
			out_key.writeObject(public_key);
			out_key.flush();

			ObjectInputStream input = new ObjectInputStream(CLA_socket.getInputStream());
			value = (BigInteger) input.readObject();
			value = rsa.decrypt(value, CLA_public_key, CLA_num);
			
			this.verify_num=value;
			out_key.close();
			input.close();
			
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		closeCLASocket();
	}
	
	
	public void casteVote(int c_ID) {

		BigInteger value = rsa.encrypt(verify_num);
		BigInteger ID = rsa.encrypt(c_ID);
		String name = rsa.encrypt(un);
		String vote = "2," + name + "," + value.toString() + "," + ID;

		try {
			System.out.println("Connecting to CTF at port in progress " + CTFport);
			CTF_socket = new Socket(server_name, CTFport);
			System.out.println("Connected to CTF");

			ObjectOutputStream op = new ObjectOutputStream(CTF_socket.getOutputStream());
			op.writeObject(vote);
			op.flush();

			ObjectInputStream ip = new ObjectInputStream(CTF_socket.getInputStream());
			String res = (String) ip.readObject();
			System.out.println(res);
		} catch (IOException | ClassNotFoundException ex) {
			ex.printStackTrace();
		}

		closeCTFSocket();

	}
	@SuppressWarnings("unchecked")
	public void viewResult() {

		try {
			System.out.println("Connecting to CTF at port in progress" + CTFport);
			CTF_socket = new Socket(server_name, CTFport);
			System.out.println("Connected to CTF");

			ObjectOutputStream out = new ObjectOutputStream(CTF_socket.getOutputStream());

			out.writeObject("3");
			out.flush();
			ObjectInputStream ip = new ObjectInputStream(CTF_socket.getInputStream());

			HashMap<String, Integer> candResult = (HashMap<String, Integer>) ip.readObject();

			for (String cand : candResult.keySet()) {
				System.out.println(cand + " has recieved " + candResult.get(cand)+ " Votes.");
			}

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		closeCTFSocket();

	}
	
	public Boolean validateUserDetails(String un, String pwd) {

		Boolean value = false;
		try {
			System.out.println("Connecting to CTF at port in progress " + CTFport);
			CTF_socket = new Socket(server_name, CTFport);

			un = rsa.encrypt(un);
			pwd = rsa.encrypt(pwd);

			ObjectOutputStream op = new ObjectOutputStream(CTF_socket.getOutputStream());

			op.writeObject("1," + un + "," + pwd);
			op.flush();
			ObjectInputStream ip = new ObjectInputStream(CTF_socket.getInputStream());

			String ctf_key = (String) ip.readObject();

			getKey(ctf_key, "CTF");
			
			ObjectOutputStream outKey = new ObjectOutputStream(CTF_socket.getOutputStream());
			outKey.writeObject(public_key);
			op.flush();
		    getCandidateList(CTF_socket);
		    
			try {

				ObjectInputStream ois = new ObjectInputStream(CTF_socket.getInputStream());

				value = (Boolean) ois.readObject();

			} catch (IOException | ClassNotFoundException ex) {
				System.err.println("Error: " + ex);
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		closeCTFSocket();
		return value;

	}
	
	
	@SuppressWarnings("unchecked")
	private void getCandidateList(Socket ctfSocket) {
		ObjectInputStream ip;
		try {
			ip = new ObjectInputStream(ctfSocket.getInputStream());
			cand_list = (HashMap<String, Integer>) ip.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	
	HashMap<String, Integer> cand_list = new HashMap<String, Integer>();
	@SuppressWarnings("rawtypes")
	private void displayCandList() {
		System.out.println("List of Candidates :");
		 Map<String, Integer> map = new TreeMap<String, Integer>(cand_list);
		 Map sortedMap = sortByValues(map);
		 Set set = sortedMap.entrySet();
		 Iterator iterator = set.iterator();
		    while(iterator.hasNext()) {
		      Map.Entry me = (Map.Entry)iterator.next();
		      System.out.println(me.getValue() + ". " + me.getKey() + "\t");
		    }
		}
	
	 public static <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map1) {
	    Comparator<K> comparator = new Comparator<K>() {
	      public int compare(K a, K b) {
	        int compare = map1.get(a).compareTo(map1.get(b));
	        if (compare == 0) 
	          return 1;
	        else 
	          return compare;
	      }
	    };
	 
	    Map<K, V> sortByValue = new TreeMap<K, V>(comparator);
	    sortByValue.putAll(map1);
	    return sortByValue;
	  }
	 
	/**
	 * @param args
	 * @throws NumberFormatException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("resource")
	public static void main(String[] args) throws NumberFormatException, IOException, ClassNotFoundException {
		
	     System.out.println( "Welcome to Secure Election System." );
	     
		Scanner src = new Scanner(System.in);
		
		String str;
		do {
			printMainMenu();
			str = src.next();

			switch (str) {

			case "2":
				System.out.println("Thank you!");
				return;

			case "1":
				System.out.println("Enter the username below :-");
				String un = src.next();
				System.out.println("Enter the password below :-");
				String pwd = src.next();

				Voter voter = new Voter();
				if (!voter.validateUserDetails(un, pwd)) {
					System.out.println("Invalid username or password. Please check your crenditials");
					continue;
				}
				voter.un = un;
				voter.pwd = pwd;
				System.out.println("\n Hello " + un + " welcome to SES voting. Please check the user menu for more details: \n");
				String choice;
				do {

					userMenu();
					choice = src.next();

					switch (choice) {
					case "1": {
						if (voter.verify_num == null) {
							voter.verifyNumber();
						}
						System.out.println("Validation number is -  " + voter.verify_num);
						
						continue;
					}
					case "2": {
						if (voter.verify_num == null) {
							System.out.println("Validation number not found for user. Get the Validation number to vote.");
							break;
						}
						
						voter.displayCandList();
						System.out.println("From the given candidates, please select one candidate to vote.");
						int vote = src.nextInt();
						voter.casteVote(vote);

						break;
					}

					case "3": {
						voter.viewResult();
						break;
					}
					case "4": {
						System.out.println("Logged out successfully.");
						userMenu();
						break;
					}

					default: {
						System.out.println("Wrong input. To logout press 4 from user menu");
						break;
					}
					}
				} while (!choice.equals("4"));

			}

		} while (!str.equals("2"));

	}
	
	/**
	 * Closes CTF Socket.
	 */
	public void closeCTFSocket() {
		try {
			CTF_socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Closes CLA Socket.
	 */
	public void closeCLASocket() {
		try {
			CLA_socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	 public static void printMainMenu()
	    {
	        System.out.println( "Please Login to Vote" );
	        System.out.println( "1.Login" );
	        System.out.println( "2.Exit" );
	    }
	    public static void userMenu()
	    {
	        System.out.println( "User Menu to vote:" );
	        System.out.println( "1.Get Validation Number to vote" );
	        System.out.println( "2.Cast Vote" );
	        System.out.println( "3.Result" );
	        System.out.println( "4.Logout" );
	    }


}
