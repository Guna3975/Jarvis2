package com.example.back2.controller;

import com.example.back2.model.Employee;
import com.example.back2.model.Leave;
import com.example.back2.service.LeaveService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("api/leave")
public class LeaveController {
    private final LeaveService leaveService;

    LeaveController(LeaveService leaveService){
        this.leaveService = leaveService;
    }

    @GetMapping("/sick")
    public List<Leave> getEmps(){
        return leaveService.getSickLeaves();
    }

    @GetMapping("/sickEmps")
    public List<Map<String,Object>> getEmpsW(){
        return leaveService.getSeickLeaveEmps();
    }

    @PostMapping("/addEmp")
    public Employee addEmp(@RequestBody Employee employee){
        return leaveService.addEmployee(employee);
    }

    @PostMapping("/add")
    public Leave addLeave(@RequestBody Leave leave){
         return leaveService.addLeave(leave);
    }

    @PutMapping("/update/{eid}")
    public Employee updLeave(@PathVariable String eid, @RequestBody Employee employee){
        return leaveService.updateBal(eid,employee);
    }

    @DeleteMapping("/delete/{lid}")
    public void del(@PathVariable String lid){
        leaveService.deleteleave(lid);
    }
}





---------

package com.example.back2.repository;

import com.example.back2.model.Leave;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Map;

public interface LeaveRepository extends MongoRepository<Leave,String> {

    @Query("{'type' : 'sick'}")
    List<Leave> findSickLeaves();

    @Aggregation(pipeline = {
            "{ '$match' : {'type' : 'sick'}}",
            "{ '$lookup' : { 'from' : 'employee', 'localField' : 'empId', 'foreignField' : '_id', 'as' : 'empDetails'}}",
            "{ '$unwind' : '$empDetails'}",
            "{ '$project' : {'leaveId' : 1, 'empId' : 1, 'type' : 1, 'status' : 1, 'empName' : '$empDetails.empName', 'leaveBal' : '$empDetails.leaveBal'}}"
    })
    List<Map<String,Object>> findSickLeavesWithEmpDetails();
}
------

package com.example.back2.service;

import com.example.back2.model.Employee;
import com.example.back2.model.Leave;
import com.example.back2.repository.EmployeeRepository;
import com.example.back2.repository.LeaveRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LeaveService {
    private final LeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;

    LeaveService(LeaveRepository leaveRepository, EmployeeRepository employeeRepository){
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
    }

    public Employee addEmployee(Employee employee){
        return employeeRepository.save(employee);
    }

    public Leave addLeave(Leave leave){
        Employee employee = employeeRepository.findById(leave.getEmpId())
                .orElseThrow(() -> new RuntimeException("No Employee Found"));

        if(employee.getLeaveBal() <= 0){
            throw new RuntimeException("No Leave Balance");
        }
        employee.setLeaveBal(employee.getLeaveBal() - 1);
        employeeRepository.save(employee);
        Leave saved = leaveRepository.save(leave);
        return saved;
    }

    public List<Leave> getSickLeaves(){
        return leaveRepository.findSickLeaves();
    }

    public List<Map<String,Object>> getSeickLeaveEmps(){
        return leaveRepository.findSickLeavesWithEmpDetails();
    }

    public Employee updateBal(String eid, Employee updatedEmp){
        Employee existingEmp = employeeRepository.findById(eid)
                .orElseThrow(()->new RuntimeException("Not Found"));
        existingEmp.setLeaveBal(updatedEmp.getLeaveBal());
        return employeeRepository.save(existingEmp);
    }

    public void deleteleave(String lid){
        leaveRepository.deleteById(lid);
    }
}


-----

1. Find sick leaves with pending status
@Aggregation(pipeline = {
    "{ '$match' : {'type' : 'sick', 'status' : 'pending'}}"
})
List<Leave> findPendingSickLeaves();

2. Find sick or casual leaves
@Aggregation(pipeline = {
    "{ '$match' : {'type' : {'$in' : ['sick', 'casual']}}}"
})
List<Leave> findSickOrCasualLeaves();

3. Find sick leaves with employee names
@Aggregation(pipeline = {
    "{ '$match' : {'type' : 'sick'}}",
    "{ '$lookup' : { 'from' : 'employee', 'localField' : 'empId', 'foreignField' : '_id', 'as' : 'empDetails'}}",
    "{ '$unwind' : '$empDetails'}",
    "{ '$project' : {'leaveId' : 1, 'empId' : 1, 'empName' : '$empDetails.empName'}}"
})
List<Map<String,Object>> findSickLeavesWithEmployeeNames();

4. Count leaves by type
@Aggregation(pipeline = {
    "{ '$group' : {'_id' : '$type', 'totalLeaves' : {'$sum' : 1}}}"
})
List<Map<String,Object>> countLeavesByType();

5. Find the total leaves for each employee
@Aggregation(pipeline = {
    "{ '$group' : {'_id' : '$empId', 'totalLeaves' : {'$sum' : 1}}}"
})
List<Map<String,Object>> countLeavesByEmployee();

6. Count only approved leaves by employee
@Aggregation(pipeline = {
    "{ '$match' : {'status' : 'approved'}}",
    "{ '$group' : {'_id' : '$empId', 'totalApproved' : {'$sum' : 1}}}"
})
List<Map<String,Object>> countApprovedLeavesByEmployee();

7. Find the top three employees by leave count
@Aggregation(pipeline = {
    "{ '$group' : {'_id' : '$empId', 'totalLeaves' : {'$sum' : 1}}}",
    "{ '$sort' : {'totalLeaves' : -1}}",
    "{ '$limit' : 3}"
})
List<Map<String,Object>> findTopThreeEmployeesByLeaveCount();

8. Using @Query()
Find sick leaves:
@Query("{'type' : 'sick'}")
List<Leave> findSickLeaves();

Find approved leaves:
@Query("{'status' : 'approved'}")
List<Leave> findApprovedLeaves();

Find leaves by employee ID:
@Query("{'empId' : ?0}")
List<Leave> findLeavesByEmpId(String empId);

Find leaves by type and status:
@Query("{'type' : ?0, 'status' : ?1}")
List<Leave> findLeavesByTypeAndStatus(String type, String status);

Find sick or casual leaves:
@Query("{'type' : {'$in' : ['sick', 'casual']}}")
List<Leave> findSickOrCasualLeaves();

Find leaves whose status is not rejected:
@Query("{'status' : {'$ne' : 'rejected'}}")
List<Leave> findNonRejectedLeaves();

Find leaves by employee ID and status:
@Query("{'empId' : ?0, 'status' : ?1}")
List<Leave> findLeavesByEmpIdAndStatus(String empId, String status);

Find all leaves sorted by leave ID in ascending order:
@Query(value = "{}", sort = "{'leaveId' : 1}")
List<Leave> findAllSortedByLeaveId();

Find leaves by type, excluding rejected leaves:
@Query("{'type' : ?0, 'status' : {'$ne' : 'rejected'}}")
List<Leave> findNonRejectedLeavesByType(String type);


Example: Leave + Employee + Department
1. LeaveRepository.java
public interface LeaveRepository extends MongoRepository<Leave, String> {

    @Aggregation(pipeline = {
        "{ '$lookup': { 'from': 'employee', 'localField': 'empId', 'foreignField': '_id', 'as': 'empDetails' } }",
        "{ '$unwind': '$empDetails' }",

        "{ '$lookup': { 'from': 'department', 'localField': 'empDetails.deptId', 'foreignField': '_id', 'as': 'deptDetails' } }",
        "{ '$unwind': '$deptDetails' }",

        "{ '$project': { 'leaveId': 1, 'type': 1, 'status': 1, 'empName': '$empDetails.empName', 'deptName': '$deptDetails.deptName' } }"
    })
    List<Map<String, Object>> getLeaveEmployeeDepartmentDetails();
}