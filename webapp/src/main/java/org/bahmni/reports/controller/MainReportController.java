package org.bahmni.reports.controller;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.exception.DRException;
import org.apache.log4j.Logger;
import org.bahmni.reports.JasperResponseConverter;
import org.bahmni.reports.api.ReportTemplates;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

import static org.bahmni.reports.api.ConfigReaderUtil.findConfig;

@Controller
public class MainReportController {

    private static final Logger logger = Logger.getLogger(MainReportController.class);
    private ReportTemplates reportTemplates;
    private JasperResponseConverter converter;

    @Autowired
    public MainReportController(ReportTemplates reportTemplates, JasperResponseConverter converter) {
        this.reportTemplates = reportTemplates;
        this.converter = converter;
    }

    @RequestMapping(value = "/report", method = RequestMethod.GET)
    public void getReport(HttpServletRequest request, HttpServletResponse response) {
        try {
            String startDate = request.getParameter("startDate");
            String endDate = request.getParameter("endDate");
            String reportName = request.getParameter("name");

            JSONObject jsonObject = findConfig(reportName, response);

            JasperReportBuilder reportBuilder = reportTemplates.get((String) jsonObject.get("type")).
                    build(jsonObject, startDate, endDate);

            String responseType = request.getParameter("responseType");
            convertToResponse(responseType, reportBuilder, response);

            response.flushBuffer();
            response.getOutputStream().close();
        } catch (SQLException | IOException e) {
            logger.error("Error running report", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }


    private void convertToResponse(String responseType, JasperReportBuilder reportBuilder, HttpServletResponse response) {
        try {
            converter.convert(responseType, reportBuilder, response);
        } catch (DRException | IOException e) {
            logger.error("Could not convert response", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}