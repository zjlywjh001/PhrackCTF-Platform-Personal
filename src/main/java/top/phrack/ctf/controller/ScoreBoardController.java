/**
 * 
 */
package top.phrack.ctf.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.SecurityUtils;
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
import top.phrack.ctf.comparators.CompareScore;
import top.phrack.ctf.models.dao.ChallengesMapper;
import top.phrack.ctf.models.services.BannedIpServices;
import top.phrack.ctf.models.services.ChallengeServices;
import top.phrack.ctf.models.services.CountryServices;
import top.phrack.ctf.models.services.SubmissionServices;
import top.phrack.ctf.models.services.UserServices;
import top.phrack.ctf.pojo.Users;
import top.phrack.ctf.pojo.Challenges;
import top.phrack.ctf.pojo.Countries;
import top.phrack.ctf.pojo.RanklistObj;
import top.phrack.ctf.pojo.ScoreBoardObj;
import top.phrack.ctf.pojo.ScoreTrend;
import top.phrack.ctf.pojo.SolveList;
import top.phrack.ctf.pojo.Submissions;
import top.phrack.ctf.utils.CommonUtils;

/**
 * 排名页面的控制器
 *
 * @author Jarvis
 * @date 2016年4月12日
 */
@Controller
public class ScoreBoardController {
	private Logger log = LoggerFactory.getLogger(ScoreBoardController.class);
	
	@Autowired
	private HttpServletRequest request;
	@Resource 
	private UserServices userServices;
	@Resource
	private BannedIpServices bannedIpServices;
	@Resource
	private SubmissionServices submissionServices;
	@Resource
	private CountryServices countryServices;
	@Resource
	private ChallengeServices challengeServices;
	
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/scoreboard", method = RequestMethod.GET)
	public ModelAndView ScoreBoard() throws Exception {
		ModelAndView mv = new ModelAndView("scoreboard");
		CommonUtils.setControllerName(request, mv);
		Subject currentUser = SecurityUtils.getSubject();
		Users userobj=CommonUtils.setUserInfo(currentUser, userServices, submissionServices,mv);
		if (userobj==null) {
			mv.addObject("thisuser","");
		} else {
			mv.addObject("thisuser",userobj.getUsername());
		}
		if (CommonUtils.CheckIpBanned(request, bannedIpServices)) {
			currentUser.logout();
		}
		
		Date currenttime= new Date();
		mv.addObject("updatetime", currenttime);
		ArrayList<ScoreBoardObj> rank = new ArrayList<ScoreBoardObj>();
		List<Users> userforrank = userServices.getUsersForRank();
		ArrayList<RanklistObj> ranklist = new ArrayList<RanklistObj>();
		List<Challenges> tasklist = challengeServices.getAllAvailChallenges();
		List<Submissions> allcorrect = submissionServices.getAllCorrectAndOrderByUserId();
		Map<Users,Long> submittable = new HashMap<Users,Long>();
		
		
		for (Users u:userforrank) {
			RanklistObj aobj = new RanklistObj();
			Submissions last = null;
			//Submissions xx = submissionServices.getLastCorrectSubmitByUserId(u.getId());
			for (Submissions tmpsub:allcorrect)
			{
				if (tmpsub.getUserid()-u.getId()==0) {
					last = tmpsub;
					submittable.put(u, Long.valueOf(allcorrect.indexOf(tmpsub)));
					break;
				}
			}
			if (last==null) {
				aobj.setLastSummit(new Date());
			} else {
				aobj.setLastSummit(last.getSubmitTime());
			}
			aobj.setuserobj(u);
			ranklist.add(aobj);

				
		}
		

		CompareScore c = new CompareScore();
		Collections.sort(ranklist,c);
		/*for (int i=0;i<ranklist.size()-1;i++) {
			for (int j=0;j<ranklist.size()-1;j++) {
				if (c.compare(ranklist.get(j),ranklist.get(j+1))==1) {
					RanklistObj tmp = ranklist.get(j);
					ranklist.set(j, ranklist.get(j+1));
					ranklist.set(j+1, tmp);
				}
			}
		}*/
		
		int count;
		count = 1;
		List<Countries> allcountry = countryServices.SelectAllCountry();
		for (RanklistObj item:ranklist) {
			if (item.getuserobj().getScore()==0) {
				continue;
			}
			ScoreBoardObj sb = new ScoreBoardObj();
			sb.setrank(count++);
			sb.setusername(item.getuserobj().getUsername());
			sb.setuserid(item.getuserobj().getId());
			sb.setscore(item.getuserobj().getScore());
			Countries usercountry = null;
			for (Countries tmpcy:allcountry)
			{
				if (tmpcy.getId()-item.getuserobj().getCountryid()==0) {
					usercountry = tmpcy;
					break;
				}
			}
			sb.setcountryname(usercountry.getCountryname());
			sb.setcountrycode(usercountry.getCountrycode());
			ArrayList<SolveList> sl = new ArrayList<SolveList>();
			for (Challenges ch:tasklist) {
				SolveList slitem = new SolveList();
				slitem.settaskid(ch.getId());
				slitem.settakstitle(ch.getTitle());
				if (submittable.containsKey(item.getuserobj())) {
					Long chindex = submittable.get(item.getuserobj());
					while (chindex<allcorrect.size() && allcorrect.get(chindex.intValue()).getUserid()-item.getuserobj().getId()==0) {
						if (allcorrect.get(chindex.intValue()).getChallengeId()-ch.getId()==0)
						{
							slitem.setsolvestr("solved");
							break;
						}
						chindex++;
					}
					if (chindex>=allcorrect.size() || allcorrect.get(chindex.intValue()).getUserid()-item.getuserobj().getId()!=0)
					{
						slitem.setsolvestr("unsolved");
					}
				} else {
					slitem.setsolvestr("unsolved");
				}
				sl.add(slitem);
			}
			sb.setsolvestat(sl);
			rank.add(sb);
		}
		
		mv.addObject("scorelist", rank);
		mv.setViewName("scoreboard");
		return mv;
	}
	
	@SuppressWarnings("unchecked")
	@ResponseBody
	@RequestMapping(value="scoretrend.json",method = {RequestMethod.GET},produces = "application/json;charset=utf-8")
	public String GetJSONscoreboard() throws Exception {
		Map<String,Object> sbobj = new HashMap<String,Object>();
		List<Users> userforrank = userServices.getUsersForRank();
		ArrayList<RanklistObj> ranklist = new ArrayList<RanklistObj>();
		List<Submissions> allcorrect = submissionServices.getAllCorrectAndOrderByUserId();
		Map<Users,Long> submittable = new HashMap<Users,Long>();

		for (Users u:userforrank) {
			RanklistObj aobj = new RanklistObj();
			Submissions last = null;
			for (Submissions tmpsub:allcorrect)
			{
				if (tmpsub.getUserid()-u.getId()==0) {
					last = tmpsub;
					submittable.put(u, Long.valueOf(allcorrect.indexOf(tmpsub)));
					break;
				}
			}
			if (last==null) {
				aobj.setLastSummit(new Date());
			} else {
				aobj.setLastSummit(last.getSubmitTime());
			}
			aobj.setuserobj(u);
			ranklist.add(aobj);
		}
		CompareScore c = new CompareScore();
		Collections.sort(ranklist,c);
		/*for (int i=0;i<ranklist.size()-1;i++) {
			for (int j=0;j<ranklist.size()-1;j++) {
				if (c.compare(ranklist.get(j),ranklist.get(j+1))==1) {
					RanklistObj tmp = ranklist.get(j);
					ranklist.set(j, ranklist.get(j+1));
					ranklist.set(j+1, tmp);
				}
			}
		}*/
		
		int count;
		count = 1;
		ArrayList<ScoreTrend> jsonsb = new ArrayList<ScoreTrend>();
		long updatetime = new Date().getTime();
		sbobj.put("lastupdate", updatetime);
		List<Challenges> allchallenges = challengeServices.getAllChallenges();
		for (RanklistObj item:ranklist) {
			if (item.getuserobj().getScore()==0) {
				continue;
			}
			ScoreTrend sbitem = new ScoreTrend();
			sbitem.setusername(item.getuserobj().getUsername());
			sbitem.setuserrank(count++);
			
			ArrayList<Submissions> usercorrect = new ArrayList<Submissions>();
			if (submittable.containsKey(item.getuserobj())) {
				Long chindex = submittable.get(item.getuserobj());
				while (chindex<allcorrect.size() && allcorrect.get(chindex.intValue()).getUserid()-item.getuserobj().getId()==0) {
					usercorrect.add(allcorrect.get(chindex.intValue()));
					chindex++;
				}
			}
			Collections.reverse(usercorrect);
			long[] scorearr = new long[usercorrect.size()];
			long[] taskarr = new long[usercorrect.size()];
			long[] datelist = new long[usercorrect.size()];
			int sum;
			sum = 0;
			for (int i=0;i<usercorrect.size();i++) {
				taskarr[i] = usercorrect.get(i).getChallengeId();
				Challenges cls = null;
				for (Challenges cl:allchallenges) {
					if (cl.getId()-taskarr[i]==0) {
						cls = cl;
					}
				}
				sum += cls.getScore();
				scorearr[i] = sum;
				datelist[i]= usercorrect.get(i).getSubmitTime().getTime();
			}
			sbitem.settimepoint(datelist);
			sbitem.settaskarr(taskarr);
			if (usercorrect.size()>0) {
				scorearr[usercorrect.size()-1] = item.getuserobj().getScore();
			}
			sbitem.setscorearr(scorearr);
			
			jsonsb.add(sbitem);
			 
		}
		
		sbobj.put("scorelist", jsonsb);
		String result = JSONObject.fromObject(sbobj).toString();
		
		return result;
		
	}
	
	
}
