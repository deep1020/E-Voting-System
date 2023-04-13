import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class CTF {

	static int port_Voter = 3403;
	static int port_CTF = 3401;

	static BigInteger Voter_public_key;
	static BigInteger Voter_Num;
	static BigInteger CLA_public_key;
	static BigInteger CLA_Num;

	HashMap<String, Boolean> hasVoted = new HashMap<String, Boolean>();
	HashMap<String, String> ValidationNum = new HashMap<String, String>();
	RSA rsa = new RSA();

	private ServerSocket CLA_serverSocket;
	private ServerSocket Voter_ServerSocket;
	private Socket VoterSocket;

	HashSet<Candidate> candidate_list = new HashSet<Candidate>();
	HashMap<String, Integer> getCandidate = new HashMap<String, Integer>();

	Boolean stop = false;

	String rsaKey = rsa.e + "," + rsa.N;

	HashMap<String, String> login = new HashMap<String, String>();
	HashMap<String, Boolean> loginVote = new HashMap<String, Boolean>();

	
	public void getKey(Socket client, String serverName) {
		try {
			ObjectInputStream ip = new ObjectInputStream(client.getInputStream());
			String key = (String) ip.readObject();
			String[] keys = key.split(",");

			if (serverName.equals("CLA")) {
				System.out.println("Here We are Getting the CLA keys.");
				CLA_public_key = new BigInteger(keys[0]);
				CLA_Num = new BigInteger(keys[1]);
				String cla_key_pair = "--CLA Public Key--" + "{" + CLA_public_key + "," + CLA_Num + "}";
				writeFile("data.txt", cla_key_pair);
			} else {
				System.out.println("Getting the Voter keys.");
				Voter_public_key = new BigInteger(keys[0]);
				Voter_Num = new BigInteger(keys[1]);
				String Voter_key_pair = "--Voter Public Key--" + "{" + Voter_public_key + "," + Voter_Num + "}";
				writeFile("data.txt", Voter_key_pair);
			}

		} catch (IOException | ClassNotFoundException ex) {
			ex.printStackTrace();
		}
	}

	
	private void writeFile(String filename, String data) {
		try {
			File fl = new File(filename);

			if (!fl.exists()) {
				fl.createNewFile();
			}
			FileWriter fw = new FileWriter(fl, true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(data);
			bw.newLine();
			bw.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	
	private void casteVote(String choose, Socket voter) {
		String[] list = choose.split(",");
		writeFile("data.txt", choose);
		BigInteger val_num = new BigInteger(list[2]);
		String un = list[1];
		un = rsa.decrypt(un, Voter_public_key, Voter_Num);
		val_num = rsa.decrypt(val_num, Voter_public_key, Voter_Num);

		BigInteger C_id = new BigInteger(list[3]);
		C_id = rsa.decrypt(C_id, Voter_public_key, Voter_Num);

		String res = "";
		try {
			ObjectOutputStream oout = new ObjectOutputStream(voter.getOutputStream());
			if (hasVoted.containsKey(un)) {
				res = "You have already voted.";
			} else if (ValidationNum.containsKey(val_num.toString())) {
				res = "Invalid Validation Number. Please recheck and try.";
			} else {
				Boolean flag = true;
				for (Candidate candidate : candidate_list) {
					if (candidate.c_ID == C_id.intValue()) {
						System.out.println("User voted for " + candidate.c_name);
						candidate.increaseVote();
						System.out.println("Vote updated for " + candidate.c_name);
						flag = false;

						res = "Your Vote for " + candidate.c_name + " has been noted.";
						hasVoted.put(un, true);
						writeUsernamePassword();
						updatecandidate_list();
						break;
					}
				}
				if (flag) {
					res = "Candidate doesn't exist. Please choose from the above list.";
				}
			}

			oout.writeObject(res);
			oout.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	private void updatecandidate_list() {
		try {
			File file = new File("candidate_list.txt");

			if (!file.exists()) {
				file.createNewFile();
			}

			else {
				file.delete();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for (Candidate candidate : candidate_list) {
				String line = candidate.c_name + "," + candidate.c_ID + "," + candidate.votesReceived;
				bw.write(line);
				bw.newLine();
				bw.flush();
			}
			bw.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	
	private void writeUsernamePassword() {
		try {
			File fl = new File("CTF_login_credentials.txt");

			if (!fl.exists()) {
				fl.createNewFile();
			} else {
				fl.delete();
			}

			FileWriter fw = new FileWriter(fl.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for (String user : login.keySet()) {
				String line = user + "," + login.get(user) + "," + hasVoted.get(user);
				bw.write(line);
				bw.newLine();
				bw.flush();
			}
			bw.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	
	@SuppressWarnings("deprecation")
	private void ReadUsernamePassword() {
		BufferedReader br = null;
		String loginPath = "./" + "CTF_login_credentials.txt";
		try {

			FileInputStream fileInput = new FileInputStream(loginPath);
			String currentline;

			br = new BufferedReader(new InputStreamReader(fileInput));

			while ((currentline = br.readLine()) != null) {
				String[] line = currentline.split(",");
				login.put(line[0], line[1]);
				loginVote.put(line[0], new Boolean(line[2]));
			}

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	private void getcandidate_list() {
		BufferedReader br = null;
		String can_List_Path = "./" + "candidate_list.txt";
		try {
			FileInputStream f = new FileInputStream(can_List_Path);
			String read;

			br = new BufferedReader(new InputStreamReader(f));

			while ((read = br.readLine()) != null) {
				String[] line_array = read.split(",");
				Candidate candidate = new Candidate(line_array[1], line_array[0], line_array[2]);
				candidate_list.add(candidate);
				getCandidate.put(candidate.getName(), candidate.c_ID);
			}

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	private void startServer() {
		/* server for CLA */
		(new Thread() {
			@SuppressWarnings("unchecked")
			public void run() {
				try {
					CLA_serverSocket = new ServerSocket(port_CTF);
					System.out.println("Here, Waiting for CLA at port no: " + port_CTF);
					Socket CLA = null;
					while (true && !stop) {
						CLA = CLA_serverSocket.accept();
						System.out.println("CLA's Server started....");

						try {
							ObjectOutputStream oout = new ObjectOutputStream(CLA.getOutputStream());
							oout.writeObject(rsaKey);
							oout.flush();
						} catch (IOException e) {
							System.err.println("Error occured" + e);
						}
						getKey(CLA, "CLA");

						ObjectInputStream input = new ObjectInputStream(CLA.getInputStream());
						ValidationNum = (HashMap<String, String>) input.readObject();
						System.out.println("Got the available validation numbers from CLA");

						CLA.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				claSocketClose();
			}

		}).start();

		/* server for voter */
		(new Thread() {
			@Override
			public void run() {

				try {
					Voter_ServerSocket = new ServerSocket(port_Voter);
					System.out.println("Here, Waiting for Voter at port no: " + port_Voter);
					while (true && !stop) {
						Socket voter = Voter_ServerSocket.accept();
						System.out.println("Voter's Server started....");

						String choice;

						ObjectInputStream in = new ObjectInputStream(voter.getInputStream());
						choice = (String) in.readObject();

						switch (choice.substring(0, 1)) {
						case "1":
							System.out.println("Voter requested to validate himself");
							try {
								ObjectOutputStream out = new ObjectOutputStream(voter.getOutputStream());
								out.writeObject(rsaKey);
								out.flush();

							} catch (IOException ex) {
								System.err.println("Error occured: " + ex);

							}
							getKey(voter, "VOTER");
							System.out.println("Sending Candidates");
							try {
								ObjectOutputStream out_list = new ObjectOutputStream(voter.getOutputStream());
								System.out.println(getCandidate);
								out_list.writeObject(getCandidate);
							} catch (IOException e) {
								e.printStackTrace();
							}

							String[] list = choice.split(",");

							String username = rsa.decrypt(list[1], Voter_public_key, Voter_Num);
							String password = rsa.decrypt(list[2], Voter_public_key, Voter_Num);

							Boolean res = false;

							try {
								ObjectOutputStream oout = new ObjectOutputStream(voter.getOutputStream());
								for (Entry<String, String> log : login.entrySet()) {
									if (log.getKey().equals(username) && log.getValue().equals(password)) {
										res = true;
										break;
									}
								}
								oout.writeObject(res);
								oout.flush();
							} catch (IOException ex) {
								ex.printStackTrace();
							}

							break;
						case "2":
							System.out.println("User selected to vote.");
							casteVote(choice, voter);
							break;

						case "3":
							System.out.println("Voter checks election results. ");

							try {
								ObjectOutputStream oout = new ObjectOutputStream(voter.getOutputStream());

								HashMap<String, Integer> result_final = new HashMap<String, Integer>();
								for (Candidate candidate : candidate_list) {
									result_final.put(candidate.c_name, candidate.votesReceived);
								}
								oout.writeObject(result_final);
								oout.flush();
							} catch (IOException e) {
								e.printStackTrace();
							}
							break;
						}
					}

					VoterSocket.close();

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

		}).start();
	}

	public void claSocketClose() {
		try {
			CLA_serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		CTF ctf = new CTF();
		ctf.ReadUsernamePassword();
		ctf.getcandidate_list();
		ctf.startServer();
	}

}
