package net.cvc_inc.cvsurveyconnect;

import android.os.Environment;

public class Constants {
    private static final String SMB_PFX = "smb://";
    private static final String PROJECTS = "192.168.1.153/Projects/";
    private static final String SURVEY_DOCS = "192.168.1.8/Office/Office/Templates/Survey Documents/";
    private static final String DEVICE_PROJECTS_FOLDER = Environment.getExternalStorageDirectory() + "/C&V Survey Connect/Projects/";
    private static final String CUTSHEET_DOC = "CVC-FieldCutSheet.xlsx";
    private static final String IMAGES_UPLOAD_FOLDER = "/Admin/Images/";
    private static final String CUTSHEETS_UPLOAD_FOLDER = "/Survey/Cutsheets/";
    private static final String POINTS_UPLOAD_FOLDER = "/Survey/Field/";
    private static final String DOMAIN = "candvdom";
    private static final String EXT_IP = "70.165.62.236";
    private static final String INT_IP = "192.168.1.8";
    private static final String HOSTNAME = "candv";
    private static final String SHARED_JOB_LIST = "SharedLetterJobList";


    private static final String PROJECTS_URL = SMB_PFX + PROJECTS;
    private static final String SURVEY_DOCS_URL = SMB_PFX + SURVEY_DOCS;
    private static final String CUTSHEET_DOC_URL = SMB_PFX + SURVEY_DOCS + CUTSHEET_DOC;
    private static final String[] ATOZ = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    public static String getSmbPfx() {
        return SMB_PFX;
    }

    public static String getDomain() {
        return DOMAIN;
    }

    public static String getExtIp() {
        return EXT_IP;
    }

    public static String getIntIp() {
        return INT_IP;
    }

    public static String getHostname() {
        return HOSTNAME;
    }

    public static String getProjectsUrl() {
        return PROJECTS_URL;
    }

    public static String getDeviceProjectsFolder() {
        return DEVICE_PROJECTS_FOLDER;
    }

    public static String getSurveyDocsURL() {
        return SURVEY_DOCS_URL;
    }

    public static String getCutsheetDoc() {
        return CUTSHEET_DOC;
    }

    public static String getCutsheetDocURL() {
        return CUTSHEET_DOC_URL;
    }

    public static String getImagessUploadFolder() {
        return IMAGES_UPLOAD_FOLDER;
    }

    public static String getCutsheetsUploadFolder() {
        return CUTSHEETS_UPLOAD_FOLDER;
    }

    public static String getPointsUploadFolder() {
        return POINTS_UPLOAD_FOLDER;
    }

    public static String[] getAtoZ() {
        return ATOZ;
    }

    public static String getSharedJobList() {
        return SHARED_JOB_LIST;
    }
}
