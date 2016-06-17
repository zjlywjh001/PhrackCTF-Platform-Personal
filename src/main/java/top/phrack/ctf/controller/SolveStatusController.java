/**
 * 
 */
package top.phrack.ctf.controller;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import top.phrack.ctf.models.services.BannedIpServices;
import top.phrack.ctf.models.services.ChallengeServices;
import top.phrack.ctf.models.services.CountryServices;
import top.phrack.ctf.models.services.SubmissionServices;
import top.phrack.ctf.models.services.UserServices;
import top.phrack.ctf.pojo.Challenges;
import top.phrack.ctf.pojo.Countries;
import top.phrack.ctf.pojo.OpLogDisp;
import top.phrack.ctf.pojo.Operatelog;
import top.phrack.ctf.pojo.SolveStat;
import top.phrack.ctf.pojo.Submissions;
import top.phrack.ctf.pojo.Users;
import top.phrack.ctf.utils.CommonUtils;

/**
 * 赛题的解题状态
 *
 * @author Jarvis
 * @date 2016年4月19日
 */
@Controller
public class SolveStatusController {
	private Logger log = LoggerFactory.getLogger(ShowInfoController.class);
	
	@Autowired
	private HttpServletRequest request;
	@Resource 
	private UserServices userServices;
	@Resource
	private BannedIpServices bannedIpServices;
	@Resource
	private SubmissionServices submissionServices;
	@Resource
	private ChallengeServices challengeServices;
	@Resource
	private CountryServices countryServices;
	
	@RequestMapping(value = "/taskstat/{id}",method = {RequestMethod.GET})
	public ModelAndView SolveStatus(@PathVariable long id) throws Exception {
		ModelAndView mv = new ModelAndView("taskstat");
		Subject currentUser = SecurityUtils.getSubject();
		CommonUtils.setControllerName(request, mv);
		CommonUtils.setUserInfo(currentUser, userServices, submissionServices,mv);
		if (CommonUtils.CheckIpBanned(request, bannedIpServices)) {
			currentUser.logout();
			return new ModelAndView("redirect:/showinfo?err=-99");
		}
		
		Challenges thischall = challengeServices.getChallengeById(id);
		mv.addObject("taskname", thischall.getTitle());
		List<Submissions> allsubs = submissionServices.getAllCorrectSubmitByTaskId(id);
		ArrayList<SolveStat> sst = new ArrayList<SolveStat>();
		long order;
		order = 1;
		if (allsubs!=null) {
			for (Submissions sb:allsubs) {
				Users passeduser= userServices.getUserById(sb.getUserid());
				Countries passcountry = countryServices.getCountryById(passeduser.getCountryid());
				if (passeduser.getRole().equals("admin")||!passeduser.getIsenabled()) {
					continue;
				}
				SolveStat ss = new SolveStat();
				ss.setorder(order++);
				ss.setsolvername(passeduser.getUsername());
				ss.setsolverid(passeduser.getId());
				ss.setcountrycode(passcountry.getCountrycode());
				ss.setcountryname(passcountry.getCountryname());
				Long submittimes = submissionServices.getsubmitTimesByUserIdAndTaskId(sb.getUserid(), id);
				System.out.println(submittimes);
				ss.setsubmits(submittimes);
				ss.settime(sb.getSubmitTime());
				sst.add(ss);
			}
		}
		
		mv.addObject("passtable", sst);
		mv.setViewName("taskstat");
		return mv;
	}
}
