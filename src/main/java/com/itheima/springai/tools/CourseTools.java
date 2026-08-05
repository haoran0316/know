package com.itheima.springai.tools;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.itheima.springai.entity.pojo.Course;
import com.itheima.springai.entity.pojo.CourseReservation;
import com.itheima.springai.entity.pojo.School;
import com.itheima.springai.entity.query.CourseQuery;
import com.itheima.springai.service.ICourseReservationService;
import com.itheima.springai.service.ICourseService;
import com.itheima.springai.service.ISchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CourseTools {
    private final ICourseService courseService;
    private final ICourseReservationService courseReservationService;
    private final ISchoolService schoolService;


    @Tool(description = "根据条件查询课程")
    public List<Course> queryCourse(@ToolParam(required = false, description = "查询条件") CourseQuery queryC){
        if(queryC == null){ // 如果为空, 返回所有课程
            return courseService.list();
        }
        QueryChainWrapper<Course> wrapper = courseService.query()
                .le(queryC.getEdu() != null, "edu", queryC.getEdu())
                .eq(queryC.getType() != null, "type", queryC.getType());
        if(queryC.getSorts() != null && !queryC.getSorts().isEmpty()){
            for(CourseQuery.Sort sort : queryC.getSorts()){
                wrapper.orderBy(true, sort.getAsc(), sort.getField());
            }
        }

        return wrapper.list();
    }

    @Tool(description = "查询所有校区")
    public List<School> querySchool(){
        return schoolService.list();
    }

    @Tool(description = "新增预约单,返回预约单号")
    public Integer createReservation(
            @ToolParam(description = "预约课程") String course,
            @ToolParam(description = "学生姓名") String studentName,
            @ToolParam(description = "联系方式") String contactInfo,
            @ToolParam(description = "预约校区") String school,
            @ToolParam(required = false, description = "备注") String remark
    ){
        CourseReservation courseReservation  = new CourseReservation();
        courseReservation.setCourse(course);
        courseReservation.setStudentName(studentName);
        courseReservation.setContactInfo(contactInfo);
        courseReservation.setSchool(school);
        courseReservation.setRemark(remark);

        courseReservationService.save(courseReservation);

        return courseReservation.getId();
    }
}
