/**
 * 
 */
package top.phrack.ctf.pojo;

/**
 * 提交列表的POJO类
 *
 * @author Jarvis
 * @date 2016年4月17日
 */
public class SubmissionList extends Submissions {
	
	private String taskname;
	
	private String user;
	
	public String gettaskname() {
		return taskname;
	}
	
	public void settaskname(String taskname) {
		this.taskname = taskname;
	}
	
	public String getuser(){
		return user;
	}
	
	public void setuser(String user) {
		this.user = user;
	}

}
