package cn.edu.buaa.se.bhac.services;

import cn.edu.buaa.se.bhac.Dao.entity.*;
import cn.edu.buaa.se.bhac.Dao.mapper.*;
import cn.edu.buaa.se.bhac.Utils.ControllerUtils;
import cn.edu.buaa.se.bhac.Utils.DaoUtils;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class BhacActivityService {

    @Autowired
    private BhacActivityMapper activityMapper;
    @Autowired
    private BhacJoinuseractivityMapper joinuseractivityMapper;
    @Autowired
    private BhacUserMapper bhacUserMapper;
    @Autowired
    private BhacBelongactivitytagMapper belongactivitytagMapper;
    @Autowired
    private BhacManageuseractivityMapper manageuseractivityMapper;

    /**
     * 该用户拥有所有权限的所有活动
     * @param user 用户
     * @return BhacActivity对象集合
     * @implNote 只返回拥有所有权限的活动（state = 0）
     */
    public String getAuthedActivities(BhacUser user, Integer pageNum, Integer limit,String title) {
        BhacUser admin = bhacUserMapper.selectById(user.getId());
        List<BhacRole> roles = admin.getRolesAct();
        List<Integer> category = new ArrayList<>();
        for (BhacRole role : roles) {
            if (role.getState() == 0 && role.getTag().getState() == 0) {
                category.add(role.getTag().getId());
            }
        }
        QueryWrapper q = new QueryWrapper();
        if(category!=null &&category.size()!=0)
            q.in("category",category);
        else {
            q.eq("id",-1);
        }
        q.like("title",title);
        q.orderByAsc("state");
        Page<BhacActivity> page = new Page<>(pageNum,limit);
        IPage<BhacActivity> iPage = activityMapper.selectPage(page,q);
        return ControllerUtils.putCountAndData(iPage,BhacActivity.class);
    }

    /**
     *  查询id 对应的活动
     * @param id 活动id
     * @return BhacActivity对象
     */
    public BhacActivity getActivity(Integer id) {
        return activityMapper.selectById(id);
    }

    /**
     * 更改活动审核状态state
     * @param id 被审核活动id
     * @param state 活动状态
     * @return true
     */
    public Boolean permitActivity(Integer id,Integer state) {
        BhacActivity activity = new BhacActivity();
        activity.setId(id);
        activity.setState(state);
        activityMapper.updateById(activity);
        return true;
    }
    
    /**
     * 返回表中字段title是(%title%),并且category = tid 的活动列表
     * @param title
     * @param tid tid = 0,则返回所有activities
     * @return  BhacActivity对象集合
     */
    public List<BhacActivity> getActivities (String title ,Integer tid,Integer pageNum,Integer limit) {
        
        QueryWrapper q = new QueryWrapper();
        if(title != null )
            q.like("title",title);
        if(tid != null && tid !=0)
            q.eq("category",tid);
        q.eq("state",1);
        Date t = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Long timestamp = LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli()/1000;
        q.gt("unix_timestamp(end)",timestamp);
        Page<BhacActivity> page = new Page<BhacActivity>(pageNum,limit);
        return DaoUtils.PageSearch(activityMapper,page,q);
    }
    
    /**
     * 让用户uid加入活动aid,并判断是否重复退出(-1)
     * @param aid
     * @param uid
     * @return 1 或者 -1
     */
    public  Integer enroll(Integer aid, Integer uid) {
        QueryWrapper q = new QueryWrapper();
        q.eq("aid",aid);
        q.eq("uid",uid);
        if(joinuseractivityMapper.selectCount(q) > 0 ) {
            return -1; // 已经加入
        }
        BhacJoinuseractivity join = new BhacJoinuseractivity();
        join.setAid(aid);
        join.setUid(uid);
        BhacActivity activity = activityMapper.selectById(aid);
        LocalDateTime dt =  activity.getDdl();
        LocalDateTime now = LocalDateTime.now();
        if(now.compareTo(dt) > 0) {
            return -2;
        }
        Integer isOpen = activity.getIsOpen();
        if(isOpen == 1) {
            join.setState(0);
            joinuseractivityMapper.insert(join);
            return 0;
        }
        joinuseractivityMapper.insert(join);
        return 1;
    }
    
    /**
     * 让用户uid退出活动aid,并判断是否重复退出(-1)
     * @param aid
     * @param uid
     * @return 1 或者 -1
     */
    public Integer unenroll(Integer aid,Integer uid) {
        QueryWrapper q = new QueryWrapper();
        q.eq("aid",aid);
        q.eq("uid",uid);
        if(joinuseractivityMapper.selectCount(q) == 0 ) {
             return -1; // 已经退出
         }
        q.eq("state",0);
        if (joinuseractivityMapper.selectCount(q) !=0) {
            joinuseractivityMapper.delete(q);
            return 0;
         }
        QueryWrapper qq = new QueryWrapper();
        qq.eq("aid",aid);
        qq.eq("uid",uid);
        joinuseractivityMapper.delete(qq);
        return 1;
    }
    
    /**
     * 查看用户id加入活动aid的信息。
     * @param aid
     * @param id
     * @return
     */
    public Object getJoinUserActivity(Integer aid,Integer id) {
        QueryWrapper q = new QueryWrapper();
        q.eq("aid",aid );
        q.eq("uid",id);
        if(joinuseractivityMapper.selectCount(q) == 0) {
            return "null";
        }
        else{
            return joinuseractivityMapper.selectOne(q);
        }
    }
    
    /**
     * org: 添加活动activity
     * @param activity
     */
    public void addActivity (BhacActivity activity)
    {
        activityMapper.insert(activity);
    }
    
    public String getMname (Integer aid)
    {
        BhacActivity activity = activityMapper.selectById(aid);
        return activity.getCategoryTag().getName();
    }
    
    public List<String> getNames (Integer aid, String mname)
    {
        BhacActivity activity = activityMapper.selectById(aid);
        List<BhacTag> tags = activity.getTagsBelong();
        List<String> names = new ArrayList<>();
        for(BhacTag tag : tags) {
            if (!tag.getName().equals(mname)) {
                names.add(tag.getName());
            }
        }
        return names;
    }
    
    public List<String> getDatesWithAct (Integer uid)
    {
        BhacUser user = bhacUserMapper.selectById(uid);
        List<BhacActivity> activities =  user.getActivitiesProcessing();
        List<BhacActivity> activities2 = user.getActivitiesSucceed();
        List<String> times = new ArrayList<>();
        
        for(BhacActivity activity : activities) {
            times.add(activity.getBegin().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        for(BhacActivity activity : activities2) {
            times.add(activity.getBegin().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return times;
    }
    
    public List<BhacActivity> getActByDate (Integer uid,String date)
    {
        date = date.substring(0,10);
        BhacUser user = bhacUserMapper.selectById(uid);
        List<BhacActivity> activities = new ArrayList<>();
        activities.addAll(user.getActivitiesProcessing());
        activities.addAll(user.getActivitiesSucceed());
        List<Integer> ids = new ArrayList<>();
        for(BhacActivity activity : activities) {
            ids.add(activity.getId());
        }
        QueryWrapper q = new QueryWrapper();
        q.eq("date(begin)",date);
        q.in("id",ids);
        return activityMapper.selectList(q);
    }
    
    public Boolean isPostedByMe (Integer uid,Integer aid)
    {
        QueryWrapper q  = new QueryWrapper();
        q.eq("uid",uid);
        q.eq("id",aid);
       return activityMapper.selectCount(q)!=0;
    }
    
    public Boolean isManagedByMe (Integer uid,Integer aid)
    {
        QueryWrapper q = new QueryWrapper();
        q.eq("uid",uid);
        q.eq("aid",aid);
        return manageuseractivityMapper.selectCount(q)!=0;
    }
    
    public List<BhacJoinuseractivity> getAllApplications (Integer aid,Integer pageNum, Integer limit)
    {
        QueryWrapper q = new QueryWrapper();
        Page<BhacJoinuseractivity> page = new Page<>(pageNum,limit);
        q.eq("aid",aid);
        q.eq("state",1);
        return DaoUtils.PageSearch(joinuseractivityMapper,page,q);
    }
    
    public int accept (Integer aid, Integer uid)
    {
        QueryWrapper q = new QueryWrapper();
        q.eq("aid",aid);
        q.eq("uid",uid);
        q.eq("state",0);
        if(joinuseractivityMapper.selectCount(q)!=0) {
            return -1;
        }
        BhacJoinuseractivity join = new BhacJoinuseractivity();
        QueryWrapper qq = new QueryWrapper();
        qq.eq("aid",aid);
        qq.eq("uid",uid);
        join.setState(0);
        joinuseractivityMapper.update(join,qq);
        return 0;
    }
    
    public void editActInfo (BhacActivity activity)
    {
        activityMapper.updateById(activity);
    }
    
    
    public Integer getId (Integer uid)
    {
        QueryWrapper q = new QueryWrapper();
        q.select("max(id) max_id").eq("uid",uid);
        List<Integer> ids = activityMapper.selectObjs(q);
        return ids.get(0);
    }
    
    public List<BhacActivity> getNotJoinActivities (QueryWrapper q)
    {
        return activityMapper.selectList(q);
    }
    
    public boolean isActivityFulled (Integer aid)
    {
        BhacActivity activity = activityMapper.selectById(aid);
        if(activity.getLimitPeopleNum()==-1) return false;
        if( activity.getUsersSucceed().size()>=activity.getLimitPeopleNum()) return true;
        return false;
    }
    
    public List<BhacActivity>  getReleasedActivities(Integer id, Integer pageNum, Integer limit)
    {
        QueryWrapper q = new QueryWrapper();
        q.eq("uid",id);
        q.orderByAsc("begin");
        Page<BhacActivity> page = new Page<>(pageNum,limit);
        return DaoUtils.PageSearch(activityMapper,page,q);
    }
    
    public boolean isConflicted (Integer aid, Integer uid)
    {
        BhacUser user = bhacUserMapper.selectById(uid);
        BhacActivity theActivity = activityMapper.selectById(aid);
        List<BhacActivity> activities = user.getActivitiesSucceed();
        activities.addAll(user.getActivitiesProcessing());
        for(BhacActivity activity: activities) {
            if(theActivity.getBegin().compareTo(activity.getEnd())<=0
                && theActivity.getEnd().compareTo(activity.getBegin())>=0)
                return true;
        }
        return false;
    }
    
    public Object getJoinedPeopleNum (Integer aid)
    {
        BhacActivity activity = activityMapper.selectById(aid);
        if(activity.getUsersSucceed()==null) {
            return 0;
        }
        return activity.getUsersSucceed().size();
    }
}
