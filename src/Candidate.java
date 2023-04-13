public class Candidate {

	String c_name;
	int c_ID;
	int votesReceived;

	public Candidate(String ID, String name, String votesReceived) {
		super();
		this.c_name = name;
		this.c_ID = Integer.parseInt(ID);
		this.votesReceived = Integer.parseInt(votesReceived);
	}

	public Candidate() {
		super();
	}

	public String getName() {
		return c_name;
	}

	public void setName(String name) {
		this.c_name = name;
	}

	public int getID() {
		return c_ID;
	}

	public void setID(int iD) {
		c_ID = iD;
	}

	public int getvotesReceived() {
		return votesReceived;
	}

	public void setvotesReceived(int votesReceived) {
		this.votesReceived = votesReceived;
	}

	public void increaseVote() {
		this.votesReceived++;
	}

}
