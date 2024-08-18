
package com.fugui.train.business.controller.admin;

import com.fugui.train.business.req.DailyTrainCarriageQueryReq;
import com.fugui.train.business.req.DailyTrainCarriageSaveReq;
import com.fugui.train.business.resp.DailyTrainCarriageQueryResp;
import com.fugui.train.business.service.DailyTrainCarriageService;
import com.fugui.train.common.commonResp.CommonResp;
import com.fugui.train.common.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/daily-train-carriage")
public class DailyTrainCarriageController {

    @Resource
    private DailyTrainCarriageService dailyTrainCarriageService;

    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody DailyTrainCarriageSaveReq req) {
        dailyTrainCarriageService.save(req);
        return new CommonResp<>();
    }

    @GetMapping("/query-list")
    public CommonResp<PageResp<DailyTrainCarriageQueryResp>> queryList(@Valid DailyTrainCarriageQueryReq req) {
        PageResp<DailyTrainCarriageQueryResp> list = dailyTrainCarriageService.queryList(req);
        return new CommonResp<>(list);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id) {
        dailyTrainCarriageService.delete(id);
        return new CommonResp<>();
    }

}
