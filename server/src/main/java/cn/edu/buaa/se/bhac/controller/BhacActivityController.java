package cn.edu.buaa.se.bhac.controller;

import cn.edu.buaa.se.bhac.Dao.entity.BhacActivity;
import cn.edu.buaa.se.bhac.Dao.entity.BhacJoinuseractivity;
import cn.edu.buaa.se.bhac.Dao.entity.BhacUser;
import cn.edu.buaa.se.bhac.Utils.ControllerUtils;
import cn.edu.buaa.se.bhac.code.ActivityCode;
import cn.edu.buaa.se.bhac.code.UserCode;
import cn.edu.buaa.se.bhac.services.BhacActivityService;
import cn.edu.buaa.se.bhac.services.BhacTagService;
import com.alibaba.fastjson.JSONObject;
import io.jsonwebtoken.Claims;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
public class BhacActivityController {
    @Autowired
    private BhacActivityService activityService;
    @Autowired
    private BhacTagService tagService;
    
    /**
     * 返回该用户可以管理的活动集
     * @param session
     * @param pageNum
     * @param limit
     * @return BhacActivities集合
     */
    @GetMapping("/admin/activities/authed")
    public String getAuthedActivities(HttpSession session, Integer pageNum, Integer limit) {
        BhacUser admin = (BhacUser) session.getAttribute("admin");
        System.out.println(activityService.getAuthedActivities(admin,pageNum,limit));
        return activityService.getAuthedActivities(admin,pageNum,limit);
    }

    /**
     * 更改活动审核状态state
     * @param id 批准的活动的aid
     * @return 返回code和message
     * @implNote 返回时请使用ControllerUtils.JsonCodeAndMessage方法
     */
    @PutMapping("/admin/activities/permit")
    public String permitActivity(@Param("id") Integer id) {
        BhacActivity activity = activityService.getActivity(id);
        if (activity == null) {
            return JSONObject.toJSONString(ControllerUtils
                  .JsonCodeAndMessage(ActivityCode.ERR_ACTIVITY_NOT_EXISTED));
        }
        if (activity.getState() != 1) {
            activityService.permitActivity(activity.getId(), 1);
        }
        return JSONObject.toJSONString(ControllerUtils.JsonCodeAndMessage(ActivityCode.SUCC_ACTIVITY_AUDIT_SUCC));
    }
    
    /**
     * 返回(%title%)且category = tid 的活动列表
     * @param title 活动标题
     * @param tid 活动分类
     * @return  List<BhacActivities>
     * @implNote  BhacActivity对象集合
     */
    @GetMapping("/untoken/activities")
    public String getActivities(@Param("title")String title, @Param("tid") Integer tid,
                                    @Param("pageNum")Integer pageNum , @Param("limit") Integer limit) {
        return JSONObject.toJSONString(
                activityService.getActivities(title,tid,pageNum,limit) ,
                ControllerUtils.filterFactory(BhacActivity.class));
    }
    
    /**
     * 返回id对应的BhacActivity
     * @param id 活动id
     * @return BhacActivity对象
     */
    @GetMapping("/untoken/activities/{id}")
    public String getActivity(@PathVariable("id") Integer id) {
        BhacActivity activity = activityService.getActivity(id);
        if (activityService.getActivity(id) == null) {
            activity = new BhacActivity();
        }
        return JSONObject.toJSONString(activity,
                ControllerUtils.filterFactory(BhacActivity.class));
    }
    
    /**
     * 用户id加入活动aid
     * @param aid 活动id
     * @param request Http 请求
     * @return 返回code 和 message
    */
    
    @PutMapping("/activities/enroll")
    public String enroll(Integer aid, HttpServletRequest request) {
        Claims claims  =  (Claims) request.getAttribute("claims");
        if(activityService.getActivity(aid) == null ) {
            return JSONObject.toJSONString(ControllerUtils.JsonCodeAndMessage(ActivityCode.ERR_ACTIVITY_NOT_EXISTED));
        }
        Integer state = activityService.enroll(aid,(Integer)claims.get("uid"));
        if(state == -1 ) {
            return JSONObject.toJSONString(ControllerUtils.JsonCodeAndMessage(UserCode.ERR_USER_ENROLLED));
        }
        return JSONObject.toJSONString(ControllerUtils.JsonCodeAndMessage(UserCode.SUCC_USER_ENROLL));
    }
    
    
    /**
     * 用户id退出活动aid
     * @param aid 活动id
     * @param request Http请求
     * @return 返回code和message
     */
    
    @PutMapping("/activities/unenroll")
    public String unenroll (Integer aid,HttpServletRequest request) {
        Claims claims  =  (Claims) request.getAttribute("claims");
        if(activityService.getActivity(aid) == null ) {
            return JSONObject.toJSONString(ControllerUtils.JsonCodeAndMessage(ActivityCode.ERR_ACTIVITY_NOT_EXISTED));
        }
        Integer state = activityService.unenroll(aid,(Integer)claims.get("uid"));
        if(state == -1) {
            return JSONObject.toJSONString(ControllerUtils.JsonCodeAndMessage(UserCode.ERR_USER_UNENROLLED));
        }
        return JSONObject.toJSONString(ControllerUtils.JsonCodeAndMessage(UserCode.SUCC_USER_UNENROLL));
    }
    
    /**
     * 根据用户id和活动id返回JoinUserActivity对象
     * @param aid
     * @param request
     * @return
     */
    @GetMapping("/join/info")
    public String getJoinInfo(Integer aid,HttpServletRequest request) {
        Claims claims  =  (Claims) request.getAttribute("claims");
        if(activityService.getActivity(aid) == null ) {
            return JSONObject.toJSONString(ControllerUtils.JsonCodeAndMessage(ActivityCode.ERR_ACTIVITY_NOT_EXISTED));
        }
        return JSONObject.toJSONString(activityService.getJoinUserActivity(aid,(Integer)claims.get("uid")));
    }
    
    /**
     * 添加活动activity
     * @param activity
     * @param request
     * @return
     */
    @PostMapping("/activities")
    public String addActivity(BhacActivity activity, HttpServletRequest request, ArrayList<Integer> tags) {
        Claims claims  =  (Claims) request.getAttribute("claims");
        activity.setUid((Integer) claims.get("uid"));
        activityService.addActivity(activity);
        if(tags!=null && tags.size()!=0) {
            Integer aid = activityService.getId((Integer) claims.get("uid"));
            tagService.addTags(tags, aid);
        }
        return JSONObject.toJSONString(ControllerUtils.JsonCodeAndMessage(ActivityCode.SUCC_ACTIVITY_ADD));
    }
    
    
    @GetMapping("/activities/getActByDate")
    public String getActByDate(HttpServletRequest request, String date) {
        Claims claims  =  (Claims) request.getAttribute("claims");
        List<BhacActivity> activities = activityService.getActByDate((Integer)claims.get("uid"),date);
        return JSONObject.toJSONString(activities,ControllerUtils.filterFactory(BhacActivity.class));
    }
    
    @GetMapping("/untoken/activities/belongs")
    public String getActTagNames(Integer aid) {
        String mname = activityService.getMname(aid);
        List<String> names = activityService.getNames(aid,mname);
        if(mname ==null) mname ="";
        HashMap<String,Object> mp = new HashMap<>();
        mp.put("categoryName",mname);
        mp.put("belongs",names);
        return JSONObject.toJSONString(ControllerUtils.JsonMap(mp));
    }
    
    @GetMapping("/activities/getDates")
    public String getDatesWithAct(HttpServletRequest request) {
        Claims claims  =  (Claims) request.getAttribute("claims");
        List<String> times = activityService.getDatesWithAct((Integer)claims.get("uid"));
        return JSONObject.toJSONString(times);
    }
    
    @GetMapping("/activities/isPostedByMe")
    public String isPostedByMe(HttpServletRequest request,Integer aid) {
        Claims claims = (Claims) request.getAttribute("claims");
        return  activityService.isPostedByMe((Integer)claims.get("uid"),aid).toString();
    }
    
    @GetMapping("/activities/isManagedByMe")
    public String IsManagedByMe(HttpServletRequest request,Integer aid) {
        Claims claims = (Claims) request.getAttribute("claims");
        return activityService.isManagedByMe((Integer)claims.get("uid"),aid).toString();
    }
    
    @GetMapping("/untoken/activities/getAllApplications")
    public String GetAllApplications(Integer aid, Integer pageNum , Integer limit) {
        List<BhacJoinuseractivity> joinuseractivities = activityService.getAllApplications(aid,pageNum,limit);
        return JSONObject.toJSONString(joinuseractivities);
    }
    
    @PutMapping("/activities/accept")
    public String Accept(Integer aid,Integer uid) {
        int state = activityService.accept(aid,uid);
        if (state == 0) {
            return JSONObject.toJSONString(ControllerUtils.JsonCodeAndMessage(ActivityCode.SUCC_ACTIVITY_ACC));
        }
        return JSONObject.toJSONString(ControllerUtils.JsonCodeAndMessage(ActivityCode.ERR_ACTIVITY_ACC_DUP));
    }
    
    @PutMapping("/activities/editInfo")
    public String editActInfo(BhacActivity activity,ArrayList<Integer> tags) {
        activityService.editActInfo(activity);
        if(tags!=null&&tags.size()!=0) {
            Integer aid = activity.getId();
            tagService.deleteTags(aid);
            tagService.addTags(tags, aid);
        }
        return JSONObject.toJSONString(ControllerUtils.JsonCodeAndMessage(ActivityCode.SUCC_ACTIVITY_UPD));
    }
//
//    @GetMapping("/admin/activities/authedCount")
//    public int getAuthedActivitiesCount(HttpSession session, Integer pageNum, Integer limit) {
//        BhacUser admin = (BhacUser) session.getAttribute("admin");
//        return activityService.getAuthedActivitiesCount(admin,pageNum,limit);
//    }
//
}
