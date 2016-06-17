/**
 * 
 */
package top.phrack.ctf.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import net.sf.json.JSONObject;
import top.phrack.ctf.models.services.BannedIpServices;
import top.phrack.ctf.models.services.CategoryServices;
import top.phrack.ctf.models.services.ChallengeServices;
import top.phrack.ctf.models.services.FileServices;
import top.phrack.ctf.models.services.HintServices;
import top.phrack.ctf.models.services.IPlogServices;
import top.phrack.ctf.models.services.SubmissionServices;
import top.phrack.ctf.models.services.UserServices;
import top.phrack.ctf.utils.CommonUtils;
import top.phrack.ctf.pojo.Attaches;
import top.phrack.ctf.pojo.Categories;
import top.phrack.ctf.pojo.TaskType;
import top.phrack.ctf.pojo.Users;
import top.phrack.ctf.pojo.Challenges;
import top.phrack.ctf.pojo.Files;
import top.phrack.ctf.pojo.Hints;
import top.phrack.ctf.pojo.Submissions;
import top.phrack.ctf.pojo.TaskHint;


/**
 * 答题页面的Controller
 *
 * @author Jarvis
 * @date 2016年4月10日
 */

@Controller
public class ChallengeController {

	private Logger log = LoggerFactory.getLogger(ChallengeController.class);
	
	
	@Resource 
	private UserServices userServices;
	@Resource
	private CategoryServices categoryServices;
	@Autowired
	private HttpServletRequest request;
	@Resource
	private BannedIpServices bannedIpServices;
	@Resource
	private ChallengeServices challengeServices;
	@Resource
	private SubmissionServices submissionServices;
	@Resource
	private FileServices fileServices;
	@Resource
	private HintServices hintServices;
	@Resource 
	private IPlogServices ipLogServices;
	
	
	@RequestMapping(value = "/challenges", method = RequestMethod.GET)
	public ModelAndView Challenges() throws Exception {
		ModelAndView mv = new ModelAndView("challenges");
		Subject currentUser = SecurityUtils.getSubject();
		Users userobj = userServices.getUserByEmail((String)currentUser.getPrincipal());
		assert(userobj!=null);
		CommonUtils.setControllerName(request, mv);
		CommonUtils.setUserInfo(currentUser, userServices, submissionServices,mv);
		if (CommonUtils.CheckIpBanned(request, bannedIpServices)) { //如果ip被ban了自动踢出去
			currentUser.logout();
			return new ModelAndView("redirect:/showinfo?err=-99");
		}
		
		ArrayList<Categories> cates = new ArrayList<Categories>();//categoryServices.selectAllCategory();
		
		
		ArrayList<TaskType> tasks = new ArrayList<TaskType>();
		List<Challenges> availchalls = challengeServices.getAllAvailChallenges();
		if (availchalls==null) {
			mv.setViewName("challenges");
			return mv;
		}
		List<Categories> allcates = categoryServices.selectAllCategory();
		List<Files> allfiles = fileServices.getAllFiles();
		List<Hints> allhints = hintServices.getAllHints();
		List<Submissions> allcorrectsubs = submissionServices.getAllCorrectAndOrderByUserId();
		List<Users> allusers = userServices.getAllUsers();
		for (Challenges ch:availchalls) {
			TaskType atask = new TaskType();
			atask.setid(ch.getId());
			Categories taskcate = null;//categoryServices.selectById(ch.getCategoryid());
			for (Categories ct:allcates) {
				if (ct.getId()-ch.getCategoryid()==0)
				{
					taskcate = ct;
					break;
				}
			}
			if (!cates.contains(taskcate))
				cates.add(taskcate);
			atask.settitle(ch.getTitle());
			atask.setcatestyle(taskcate.getMark());
			atask.setcontent(ch.getDescription());
			String paramid = request.getParameter("taskid");
			if (paramid!=null) {
				mv.addObject("nowtask","?taskid="+paramid);
				if (Long.valueOf(paramid).longValue()==ch.getId().longValue()) {
					String watched = ch.getWatchby();
					
					if (watched==null) {
						ch.setWatchby(String.valueOf(userobj.getId()));
						challengeServices.updateChallenge(ch);
					} else {
						String[] tmp = watched.split("\\|");
						if (!ArrayUtils.contains(tmp, String.valueOf(userobj.getId()))) {
							watched = watched+"|"+String.valueOf(userobj.getId());
							ch.setWatchby(watched);
							challengeServices.updateChallenge(ch);
						}
					}
				}
			}
			
			if (paramid!=null && StringUtils.isNumeric(paramid) && Long.valueOf(paramid).equals(ch.getId())) {
				atask.setin("in");
			} else {
				atask.setin("");
			}
			String readlist = ch.getWatchby();
			if (readlist==null) readlist="";
			String[] readby = readlist.split("\\|");
			if (ArrayUtils.contains(readby, String.valueOf(userobj.getId()))) {
				atask.setisnew(false);
			} else {
				atask.setisnew(true);
			}
			atask.setscore(ch.getScore());
			atask.setcate(taskcate.getName());
			//ArrayList<Files> taskfile = new ArrayList<Files>();//fileServices.getFilesByChallengeId(ch.getId());
			ArrayList<Attaches> att = new ArrayList<Attaches>();
			
			for (Files f:allfiles) {
				if (f.getChallengeid()!=null && f.getChallengeid()-ch.getId()==0) {
					Attaches taskatt = new Attaches();
					taskatt.setname(f.getFilename());
					taskatt.seturl(f.getResindex());
					att.add(taskatt);
				}
			}
			atask.setattach(att);
			
			//List<Hints> taskhints = hintServices.getHintsByTaskId(ch.getId());
			ArrayList<TaskHint> hintsarr = new ArrayList<TaskHint>();
			int num = 0;
			for (Hints h:allhints) {
				if (h.getChallengeid()-ch.getId()==0) {
					TaskHint th = new TaskHint();
					th.setlabel(++num);
					th.setcontent(h.getContent());
					hintsarr.add(th);
				}
			}
			atask.sethints(hintsarr);
			//List<Submissions> correctsubs = submissionServices.getAllCorrectSubmitByTaskId(ch.getId());
			Map<Long,Long> solves = new HashMap<Long,Long>();
			int solvepeoples = 0;
			for (Submissions sb:allcorrectsubs) {
				if (!solves.containsKey(sb.getUserid()) && sb.getChallengeId()-ch.getId()==0) {
					Users solveuser = null;
					for (Users u:allusers) {
						if (u.getId()-sb.getUserid()==0) {
							solveuser = u;
							break;
						}
					}
					if (solveuser!=null ) {
						if (!solveuser.getRole().equals("admin")) {
							solvepeoples++;
						}
						solves.put(solveuser.getId(), Long.valueOf(1));
					}
					
				}
			}
			atask.setsolvenum(solvepeoples);
			if (solves.containsKey(userobj.getId()))
				atask.setsolved(true);
			else  
				atask.setsolved(false);
			Date nowtime = new Date();
			if (nowtime.after(ch.getInvalidate())) {
				atask.setfin(true);
			} else {
				atask.setfin(false);
			}
			if (atask.getisnew()) {
				atask.setstat("danger");
			} else {
				if (atask.getsolved()) {
					atask.setstat("success");
				} else {
					atask.setstat("default");
				}
			}
			tasks.add(atask);
		}
		mv.addObject("cates",cates);
		mv.addObject("tasklist",tasks);
		
		mv.setViewName("challenges");
		return mv;
	}
	
	@ResponseBody
	@RequestMapping(value="/submitanswer.json",method={RequestMethod.POST},produces="application/json;charset=utf-8")
	public String CheckSubmitAnswer() throws Exception {
		Map<String,String> resp = new HashMap<String,String>();
		String submited_flag = request.getParameter("flag");
		String taskid = request.getParameter("taskid");
		
		Subject currentUser = SecurityUtils.getSubject();
		Users userobj = userServices.getUserByEmail((String)currentUser.getPrincipal());
		if (userobj==null) {
			resp.put("errmsg", "Session time out!! Please login again !!");
			resp.put("err", "-98");
			String result = JSONObject.fromObject(resp).toString();
			return result;
		}
		assert(userobj!=null);
		if (CommonUtils.CheckIpBanned(request, bannedIpServices)) { //如果ip被ban了自动踢出去
			currentUser.logout();
			resp.put("errmsg", "Your IP has been banned!!");
			resp.put("err", "-100");
			String result = JSONObject.fromObject(resp).toString();
			return result;
		}
		
		if (taskid==null || !StringUtils.isNumeric(taskid)) {
			resp.put("errmsg", "Invalid_Parameter!");
			resp.put("err", "-1");
			String result = JSONObject.fromObject(resp).toString();
			return result;
		}
		
		if (!userobj.getIsenabled()) {
			resp.put("errmsg", "Your account has been banned!");
			resp.put("err", "-100");
			String result = JSONObject.fromObject(resp).toString();
			return result;
		}
		
		
		if (!currentUser.isAuthenticated() && !currentUser.isRemembered()) {
			resp.put("errmsg", "Session time out!! Please login again !!");
			resp.put("err", "-98");
			String result = JSONObject.fromObject(resp).toString();
			return result;
		}

		Challenges taskobj = challengeServices.getChallengeById(Long.valueOf(taskid));
		Date nowtime = new Date();
		if (!taskobj.getExposed()||nowtime.before(taskobj.getAvailable())|| nowtime.after(taskobj.getInvalidate())) {
			resp.put("errmsg", "Something wrong! This challenge is no longer available.");
			resp.put("err", "-99");
			String result = JSONObject.fromObject(resp).toString();
			CommonUtils.storeUserIpUsageInfo(request, userServices, ipLogServices, userobj.getEmail());
			return result;
		}
		Long subs = submissionServices.getSolvedByUseridAndTaskId(userobj.getId(),Long.valueOf(taskid));
		if (subs>0) {
			resp.put("errmsg", "You have solved this Task! DO NOT Submit again!");
			resp.put("err", "-2");
			String result = JSONObject.fromObject(resp).toString();
			CommonUtils.storeUserIpUsageInfo(request, userServices, ipLogServices, userobj.getEmail());
			return result;
		}
		
		Submissions lastsub = submissionServices.getLastSubmitByUserId(Long.valueOf(userobj.getId()));
		long currenttime = nowtime.getTime();
		if (lastsub!=null && currenttime-lastsub.getSubmitTime().getTime()<=30000) {
			resp.put("errmsg", "Submit too fast !Please slow down! You can only submit every 30 seconds!");
			resp.put("err", "-3");
			String result = JSONObject.fromObject(resp).toString();
			CommonUtils.storeUserIpUsageInfo(request, userServices, ipLogServices, userobj.getEmail());
			return result;
		}
		
		Submissions asub = new Submissions();
		asub.setChallengeId(taskobj.getId());
		asub.setSubmitTime(new Date());
		asub.setUserid(userobj.getId());
		String Compareflag = new SimpleHash("SHA-256",submited_flag,CommonUtils.getFlagSalt()).toHex();
		if (Compareflag.equals(taskobj.getFlag())) {
			resp.put("errmsg", "Correct Answer!!Congratulations!");
			resp.put("err", "0");
			asub.setContent(Compareflag);
			asub.setCorrect(true);
			List<Submissions> corsub = submissionServices.getAllCorrectSubmitByTaskId(taskobj.getId());
			Map<Long,Long> solves = new HashMap<Long,Long>();
			int solvepeoples = 0;
			List<Users> usersforsolves = userServices.getUsersForRank();
			for (Submissions sb:corsub) {
				if (solves.get(sb.getUserid())==null) {
					Users sbuserobj = null;
					for (Users u:usersforsolves) {
						
						if (u.getId().longValue()==sb.getUserid().longValue()) {
							sbuserobj = u;
							break;
						}
					}
					if (sbuserobj!=null) {
						solvepeoples++;
						solves.put(sb.getUserid(), Long.valueOf(1));
					}
				}
			}
			if (userobj.getRole().equals("admin"))
				resp.put("solves", String.valueOf(solvepeoples));
			else 
				resp.put("solves", String.valueOf(solvepeoples+1));
			long score = userobj.getScore()+taskobj.getScore();
			userobj.setScore(score);
			resp.put("newscore", String.valueOf(score));
			userServices.updateUser(userobj);
			submissionServices.insertNewSubmission(asub);
			resp.put("newrank", String.valueOf(CommonUtils.getUserrank(userobj,userServices,submissionServices)));
			String result = JSONObject.fromObject(resp).toString();
			CommonUtils.storeUserIpUsageInfo(request, userServices, ipLogServices, userobj.getEmail());
			return result;
		} else {
			resp.put("errmsg", "Wrong Answer!!Please try again!");
			resp.put("err", "-4");
			asub.setContent(CommonUtils.XSSFilter(submited_flag));
			asub.setCorrect(false);
		}
		
		submissionServices.insertNewSubmission(asub);
		CommonUtils.storeUserIpUsageInfo(request, userServices, ipLogServices, userobj.getEmail());
		String result = JSONObject.fromObject(resp).toString();
		return result;
		
	}
	
		
}
