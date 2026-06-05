package com.example.JaspertReport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app")
public class TenantProperties {

    private String profile = "centralized";
    private String assignedTenant;
    private Map<String, Tenant> tenants = new HashMap<>();

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getAssignedTenant() {
        return assignedTenant;
    }

    public void setAssignedTenant(String assignedTenant) {
        this.assignedTenant = assignedTenant;
    }

    public Map<String, Tenant> getTenants() {
        return tenants;
    }

    public void setTenants(Map<String, Tenant> tenants) {
        this.tenants = tenants;
    }

    public static class Tenant {

        private String serviceToken;
        private String reportesRuta;
        private Datasource datasource = new Datasource();
        // null = not configured → allow all (backward compatibility)
        // empty list = explicitly configured empty → block all (spec A3)
        private List<String> allowedReports;
        // null = not configured → allow all formats
        private List<String> allowedFormats;

        public String getServiceToken() {
            return serviceToken;
        }

        public void setServiceToken(String serviceToken) {
            this.serviceToken = serviceToken;
        }

        public String getReportesRuta() {
            return reportesRuta;
        }

        public void setReportesRuta(String reportesRuta) {
            this.reportesRuta = reportesRuta;
        }

        public Datasource getDatasource() {
            return datasource;
        }

        public void setDatasource(Datasource datasource) {
            this.datasource = datasource;
        }

        public List<String> getAllowedReports() {
            return allowedReports;
        }

        public void setAllowedReports(List<String> allowedReports) {
            this.allowedReports = allowedReports;
        }

        public List<String> getAllowedFormats() {
            return allowedFormats;
        }

        public void setAllowedFormats(List<String> allowedFormats) {
            this.allowedFormats = allowedFormats;
        }
    }

    public static class Datasource {

        private String url;
        private String username;
        private String password;
        private String driverClassName = "com.mysql.cj.jdbc.Driver";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
    }
}
