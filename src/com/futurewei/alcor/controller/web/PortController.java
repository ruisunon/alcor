package com.futurewei.alcor.controller.web;

import com.futurewei.alcor.controller.app.onebox.OneBoxConfig;
import com.futurewei.alcor.controller.app.onebox.OneBoxUtil;
import com.futurewei.alcor.controller.cache.repo.PortRedisRepository;
import com.futurewei.alcor.controller.cache.repo.SubnetRedisRepository;
import com.futurewei.alcor.controller.cache.repo.VpcRedisRepository;
import com.futurewei.alcor.controller.exception.ParameterNullOrEmptyException;
import com.futurewei.alcor.controller.exception.ParameterUnexpectedValueException;
import com.futurewei.alcor.controller.exception.ResourceNotFoundException;
import com.futurewei.alcor.controller.exception.ResourceNullException;
import com.futurewei.alcor.controller.model.PortState;
import com.futurewei.alcor.controller.model.PortStateGroup;
import com.futurewei.alcor.controller.web.util.RestPreconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.web.bind.annotation.RequestMethod.*;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;

@RestController
public class PortController {

    @Autowired
    private VpcRedisRepository vpcRedisRepository;

    @Autowired
    private SubnetRedisRepository subnetRedisRepository;

    @Autowired
    private PortRedisRepository portRedisRepository;

    @RequestMapping(
            method = GET,
            value = {"/project/{projectid}/port/{portId}", "v4/{projectid}/ports/{portId}"})
    public PortState getPortStateById(@PathVariable String projectid, @PathVariable String portId) throws Exception {

        PortState portState = null;

        try{
            RestPreconditions.verifyParameterNotNullorEmpty(projectid);
            RestPreconditions.verifyParameterNotNullorEmpty(portId);
            RestPreconditions.verifyResourceFound(projectid);

            portState = this.portRedisRepository.findItem(portId);
        }catch (ParameterNullOrEmptyException e){
            //TODO: REST error code
            throw new Exception(e);
        }

        if(portState == null){
            //TODO: REST error code
            return new PortState();
        }

        return portState;
    }

    @RequestMapping(
            method = POST,
            value = {"/project/{projectid}/port", "v4/{projectid}/ports"})
    @ResponseStatus(HttpStatus.CREATED)
    public PortState createPortState(@PathVariable String projectid, @RequestBody PortState resource) throws Exception {

        long T0 = System.nanoTime();

        try{
            RestPreconditions.verifyParameterNotNullorEmpty(projectid);
            RestPreconditions.verifyResourceNotNull(resource);
            RestPreconditions.verifyResourceFound(projectid);

            // TODO: Create a verification framework for all resources
            RestPreconditions.verifyResourceFound(resource.getNetworkId());
            RestPreconditions.populateResourceProjectId(resource, projectid);

            this.portRedisRepository.addItem(resource);
            long T1 = System.nanoTime();

            if(OneBoxConfig.IS_Demo) {
                long[] times = OneBoxUtil.CreatePort(resource);
                RestPreconditions.recordRequestTimeStamp(resource.getId(), T0, T1, times);
            }
        }
        catch (ResourceNullException e){
            throw new Exception(e);
        }

        return new PortState(resource);
    }

    @RequestMapping(
            method = PUT,
            value = {"/project/{projectid}/port/{portid}", "v4/{projectid}/ports/{portid}"})
    public PortState updateSubnetState(@PathVariable String projectid, @PathVariable String portid, @RequestBody PortState resource) throws Exception {

        PortState portState = null;

        try{
            RestPreconditions.verifyParameterNotNullorEmpty(projectid);
            RestPreconditions.verifyParameterNotNullorEmpty(portid);
            RestPreconditions.verifyResourceNotNull(resource);

            RestPreconditions.verifyResourceFound(resource.getNetworkId());
            RestPreconditions.populateResourceProjectId(resource, projectid);

            portState = this.portRedisRepository.findItem(portid);
            if(portState == null){
                throw new ResourceNotFoundException("Port not found : " + portid);
            }

            RestPreconditions.verifyParameterEqual(portState.getProjectId(), projectid);

            this.portRedisRepository.addItem(resource);
            portState = this.portRedisRepository.findItem(portid);

        }catch (ParameterNullOrEmptyException e){
            throw new Exception(e);
        }catch (ResourceNotFoundException e){
            throw new Exception(e);
        }catch (ParameterUnexpectedValueException e){
            throw new Exception(e);
        }

        return portState;
    }

    @RequestMapping(
            method = DELETE,
            value = {"/project/{projectid}/port/{portid}", "v4/{projectid}/ports/{portid}"})
    public void deletePortState(@PathVariable String projectid, @PathVariable String portid) throws Exception {

        PortState portState = null;

        try {
            RestPreconditions.verifyParameterNotNullorEmpty(projectid);
            RestPreconditions.verifyParameterNotNullorEmpty(portid);

            portState = this.portRedisRepository.findItem(portid);
            if(portState == null){
                return;
            }

            RestPreconditions.verifyParameterEqual(portState.getProjectId(), projectid);

            portRedisRepository.deleteItem(portid);

        }catch (ParameterNullOrEmptyException e){
            throw new Exception(e);
        }catch (ParameterUnexpectedValueException e){
            throw new Exception(e);
        }
    }

    @RequestMapping(
            method = GET,
            value = "/project/{projectid}/subnet/{subnetid}/ports")
    public Map gePortStatesByProjectIdAndSubnetId(@PathVariable String projectid, @PathVariable String subnetid) throws Exception {
        Map<String, PortState> portStates = null;

        try {
            RestPreconditions.verifyParameterNotNullorEmpty(projectid);
            RestPreconditions.verifyParameterNotNullorEmpty(subnetid);
            RestPreconditions.verifyResourceFound(projectid);
            RestPreconditions.verifyResourceFound(subnetid);

            portStates = this.portRedisRepository.findAllItems();
            portStates = portStates.entrySet().stream()
                    .filter(state -> projectid.equalsIgnoreCase(state.getValue().getProjectId())
                            && subnetid.equalsIgnoreCase(state.getValue().getNetworkId()))
                    .collect(Collectors.toMap(state -> state.getKey(), state-> state.getValue()));

        }catch (ParameterNullOrEmptyException e){
            throw new Exception(e);
        }catch (ResourceNotFoundException e){
            throw new Exception(e);
        }

        return portStates;
    }

    @RequestMapping(
            method = POST,
            value = {"/project/{projectid}/portgroup"},
            consumes="application/json",
            produces="application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public List<PortState> createPortStates(@PathVariable String projectid, @RequestBody PortStateGroup resourceGroup) throws Exception {

        long T0 = System.nanoTime();
        List<PortState> response = new ArrayList<>();

        try{
            RestPreconditions.verifyParameterNotNullorEmpty(projectid);
            RestPreconditions.verifyResourceFound(projectid);

            List<PortState> portStates = resourceGroup.getPortStates();
            for (PortState state : portStates) {
                this.portRedisRepository.addItem(state);
                response.add(state);
            }
            long T1 = System.nanoTime();

            if(OneBoxConfig.IS_Demo) {
                long[][] elapsedTimes = OneBoxUtil.CreatePortGroup(resourceGroup);
                int hostCount = elapsedTimes.length;

                long averageElapseTime = 0, minElapseTime = Long.MAX_VALUE, maxElapseTime = Long.MIN_VALUE;
                System.out.println("Total number of time sequences:" + hostCount);
                for (int i = 0; i < hostCount; i++) {
                    long et = elapsedTimes[i][2] - T0;
                    averageElapseTime += et;
                    if(et<minElapseTime) minElapseTime=et;
                    if(et>maxElapseTime) maxElapseTime=et;
                    RestPreconditions.recordRequestTimeStamp(resourceGroup.getPortState(i).getId(), T0, T1, elapsedTimes[i]);
                }

                OneBoxConfig.TIME_STAMP_WRITER.newLine();
                OneBoxConfig.TIME_STAMP_WRITER.write("," + averageElapseTime/(1000000*hostCount) + "," +  minElapseTime/1000000 + "," + maxElapseTime/1000000);
                OneBoxConfig.TIME_STAMP_WRITER.newLine();
                OneBoxConfig.TIME_STAMP_WRITER.write("Average time of " + OneBoxConfig.TOTAL_REQUEST + " requests :" + OneBoxConfig.TOTAL_TIME/OneBoxConfig.TOTAL_REQUEST + " ms");
                if(OneBoxConfig.TIME_STAMP_WRITER != null)
                    OneBoxConfig.TIME_STAMP_WRITER.close();

            }
        }
        catch (Exception e){
            throw e;
        }

        return response;
    }

}