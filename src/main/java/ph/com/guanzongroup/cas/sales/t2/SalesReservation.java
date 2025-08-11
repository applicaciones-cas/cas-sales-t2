package ph.com.guanzongroup.cas.sales.t2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.agent.ShowDialogFX;
import org.guanzon.appdriver.agent.services.Model;
import org.guanzon.appdriver.agent.services.Transaction;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.guanzon.appdriver.constant.RecordStatus;
import org.guanzon.appdriver.constant.UserRight;
import org.guanzon.appdriver.iface.GValidator;
import org.guanzon.cas.client.Client;
import org.guanzon.cas.client.services.ClientControllers;
import org.guanzon.cas.inv.Inventory;
import org.guanzon.cas.inv.services.InvControllers;
import org.guanzon.cas.parameter.Branch;
import org.guanzon.cas.parameter.Brand;
import org.guanzon.cas.parameter.services.ParamControllers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import ph.com.guanzongroup.cas.sales.model.Model_Sales_Reservation_Detail;
import ph.com.guanzongroup.cas.sales.model.Model_Sales_Reservation_Master;
import ph.com.guanzongroup.cas.sales.services.SalesReservationModels;
import ph.com.guanzongroup.cas.sales.status.Sales_Reservation_Static;
import ph.com.guanzongroup.cas.sales.t1.SalesInquiry;
import ph.com.guanzongroup.cas.sales.t1.services.SalesControllers;
import ph.com.guanzongroup.cas.sales.validator.Sales_Reservation_Validator_Factory;

public class SalesReservation extends Transaction {

    List<Model_Sales_Reservation_Master> poSalesReservationMaster;
  
    List<Model> paDetailRemoved;
    public JSONObject InitTransaction() throws SQLException, GuanzonException {
        SOURCE_CODE = "srsv";

        poMaster = new SalesReservationModels(poGRider).Sales_Reservation_Master();
        poDetail = new SalesReservationModels(poGRider).Sales_Reservation_Detail();
        paDetail = new ArrayList<>();
        return initialize();
    }
    private String psIndustryId = "";
    private String psCompanyId = "";
    private String psCategoryCd = "";
    
    @Override
    public JSONObject initFields() {
        //Put initial model values here/
        poJSON = new JSONObject();
        try {
            poJSON = new JSONObject();
            Master().setBranchCode(poGRider.getBranchCode());
            Master().setIndustryID(psIndustryId);
            Master().setCompanyID(psCompanyId);
            Master().setCategoryCode(psCategoryCd);
            Master().setTransactionDate(poGRider.getServerDate());
            Master().setTransactionStatus(Sales_Reservation_Static.OPEN);

        } catch (SQLException ex) {
            Logger.getLogger(SalesReservation.class
                    .getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
            poJSON.put("result", "error");
            poJSON.put("message", MiscUtil.getException(ex));
            return poJSON;
        }
        poJSON.put("result", "success");
        return poJSON;
    }

    public void setIndustryID(String industryID) {
        psIndustryId = industryID;
    }

    public void setCompanyID(String companyID) {
        psCompanyId = companyID;
    }

    public void setCategoryCd(String categoryCD) {
        psCategoryCd = categoryCD;
    }

    public JSONObject NewTransaction() throws CloneNotSupportedException {
        return newTransaction();
    }

    public JSONObject SaveTransaction() throws SQLException, GuanzonException, CloneNotSupportedException {
        return saveTransaction();
    }

    public JSONObject OpenTransaction(String transactionNo) throws CloneNotSupportedException, SQLException, GuanzonException {
        resetMaster();
        resetOthers();
        Detail().clear();
        return openTransaction(transactionNo);
    }

    public JSONObject UpdateTransaction() {
        return updateTransaction();
    }

    

    public JSONObject CancelTransaction(String remarks) throws ParseException, SQLException, CloneNotSupportedException, GuanzonException {
        poJSON = new JSONObject();

        String lsStatus = Sales_Reservation_Static.CANCELLED;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(Sales_Reservation_Static.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            } else {
                if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "User is not an authorized approving officer..");
                    return poJSON;
                }
            }
        }
//        poJSON = setValueToOthers(lsStatus);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            return poJSON;
//        }
        //check  the user level again then if he/she allow to approve
        poGRider.beginTrans("UPDATE STATUS", "CancelTransaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
//        poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            poGRider.rollbackTrans();
//            return poJSON;
//        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction canceelled successfully.");
        } else {
            poJSON.put("message", "Transaction canceelled request submitted successfully.");
        }

        return poJSON;
    }

    

    public JSONObject ConfirmTransaction(String remarks) throws ParseException, SQLException, CloneNotSupportedException, GuanzonException {
        poJSON = new JSONObject();

        String lsStatus = Sales_Reservation_Static.CONFIRMED;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(Sales_Reservation_Static.CONFIRMED);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            } else {
                if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "User is not an authorized approving officer..");
                    return poJSON;
                }
            }
        }
//        poJSON = setValueToOthers(lsStatus);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            return poJSON;
//        }
        //check  the user level again then if he/she allow to approve
        poGRider.beginTrans("UPDATE STATUS", "ConfirmTransaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
//        poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            poGRider.rollbackTrans();
//            return poJSON;
//        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction confirmed successfully.");
        } else {
            poJSON.put("message", "Transaction confirmation request submitted successfully.");
        }

        return poJSON;
    }

    public JSONObject PaidTransaction(String remarks) throws ParseException, SQLException, CloneNotSupportedException, GuanzonException {
        poJSON = new JSONObject();

        String lsStatus = Sales_Reservation_Static.PAID;
        boolean lbConfirm = true;

        if (getEditMode() != EditMode.READY) {
            poJSON.put("result", "error");
            poJSON.put("message", "No transacton was loaded.");
            return poJSON;
        }

        if (lsStatus.equals((String) poMaster.getValue("cTranStat"))) {
            poJSON.put("result", "error");
            poJSON.put("message", "Transaction was already confirmed.");
            return poJSON;
        }

        //validator
        poJSON = isEntryOkay(Sales_Reservation_Static.PAID);
        if (!"success".equals((String) poJSON.get("result"))) {
            return poJSON;
        }

        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
            poJSON = ShowDialogFX.getUserApproval(poGRider);
            if (!"success".equals((String) poJSON.get("result"))) {
                return poJSON;
            } else {
                if (Integer.parseInt(poJSON.get("nUserLevl").toString()) <= UserRight.ENCODER) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "User is not an authorized approving officer..");
                    return poJSON;
                }
            }
        }
//        poJSON = setValueToOthers(lsStatus);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            return poJSON;
//        }
        //check  the user level again then if he/she allow to approve
        poGRider.beginTrans("UPDATE STATUS", "PaidTransaction", SOURCE_CODE, Master().getTransactionNo());

        poJSON = statusChange(poMaster.getTable(), (String) poMaster.getValue("sTransNox"), remarks, lsStatus, !lbConfirm, true);
        if (!"success".equals((String) poJSON.get("result"))) {
            poGRider.rollbackTrans();
            return poJSON;
        }
//        poJSON = saveUpdates(PurchaseOrderStatus.CONFIRMED);
//        if (!"success".equals((String) poJSON.get("result"))) {
//            poGRider.rollbackTrans();
//            return poJSON;
//        }

        poGRider.commitTrans();

        poJSON = new JSONObject();
        poJSON.put("result", "success");

        if (lbConfirm) {
            poJSON.put("message", "Transaction Paid successfully.");
        } else {
            poJSON.put("message", "Transaction Paid request submitted successfully.");
        }

        return poJSON;
    }


    

    public JSONObject AddDetail() throws CloneNotSupportedException {
        if (Detail(getDetailCount() - 1).getStockID().isEmpty()) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "Last row has empty item.");
            return poJSON;
        }

        return addDetail();
    }


    /*Search Master References*/
    public JSONObject SearchBranch(String value, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Branch object = new ParamControllers(poGRider, logwrapr).Branch();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
            Master().setBranchCode(object.getModel().getBranchCode());
        }

        return poJSON;
    }

    public JSONObject SearchClient(String value, boolean byCode)
            throws SQLException,
            GuanzonException {
        poJSON = new JSONObject();
        if(value.isEmpty()){
             Master().setClientID(null);
        }

        Client object = new ClientControllers(poGRider, logwrapr).Client();
        object.Master().setRecordStatus(RecordStatus.ACTIVE);
        object.Master().setClientType("1");
        poJSON = object.Master().searchRecord(value, byCode);
        if ("success".equals((String) poJSON.get("result"))) {
            Master().setClientID(object.Master().getModel().getClientId());
            Master().setClientID(Master().Client_Address().getAddressId());
//            Master().setContactID(object.Mobile().getModel().getClientId());
        }

        return poJSON;
    }
    public JSONObject SearchBrand(String value, boolean byCode, int row) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Brand brand = new ParamControllers(poGRider, logwrapr).Brand();
        brand.getModel().setRecordStatus(RecordStatus.ACTIVE);

        poJSON = brand.searchRecord(value, byCode, Master().getIndustryID());

        if ("success".equals((String) poJSON.get("result"))) {
            Detail(row).setBrandId(brand.getModel().getBrandId());
        }

        return poJSON;
    }
    
    
    public JSONObject SearchModel(String value, boolean byCode, int row)
            throws SQLException, GuanzonException, NullPointerException, CloneNotSupportedException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.getModel().setRecordStatus(RecordStatus.ACTIVE);

        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = Master().getIndustryID().isEmpty() ? null : Master().getIndustryID();
        String category = Master().getCategoryCode();

        poJSON = object.searchRecord(
                value,
                byCode,
                null,
                brand,
                industry,
                category
        );

        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Detail(lnRow).getStockID().equals("") || Detail(lnRow).getStockID() == null)
                            || (Detail(lnRow).getStockID().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Barcode: " + object.getModel().getDescription() + " already exist in table at row " + (lnRow + 1) + ".");
                        poJSON.put("tableRow", lnRow);
                        return poJSON;
                    }
                }
            }
            
            Detail(row).setStockID(object.getModel().getStockId());
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
            if(row == getDetailCount() - 1){
                AddDetail();
            }
        }
        return poJSON;
    }
    public JSONObject SearchBarcode(String value, boolean byCode, int row)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {

        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = Master().getIndustryID().isEmpty() ? null : Master().getIndustryID();
        String category = Master().getCategoryCode();

        poJSON = object.searchRecord(
                value,
                byCode,
                null,
                brand,
                industry,
                category
        );

        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Detail(lnRow).getStockID().equals("") || Detail(lnRow).getStockID() == null)
                            || (Detail(lnRow).getStockID().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Barcode: " + object.getModel().getDescription() + " already exist in table at row " + (lnRow + 1) + ".");
                        poJSON.put("tableRow", lnRow);
                        return poJSON;
                    }
                }
            }
            
            Detail(row).setStockID(object.getModel().getStockId());
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
            if(row == getDetailCount() - 1){
                AddDetail();
            }
        }
        return poJSON;
    }
    public JSONObject SearchDescription(String value, boolean byCode, int row)
            throws ExceptionInInitializerError, SQLException, GuanzonException, CloneNotSupportedException, NullPointerException {

        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus(RecordStatus.ACTIVE);

        String brand = (Detail(row).getBrandId() != null && !Detail(row).getBrandId().isEmpty()) ? Detail(row).getBrandId() : null;
        String industry = Master().getIndustryID().isEmpty() ? null : Master().getIndustryID();
        String category = Master().getCategoryCode();

        poJSON = object.searchRecord(
                value,
                byCode,
                null,
                brand,
                industry,
                category
        );

        if ("success".equals((String) poJSON.get("result"))) {
            for (int lnRow = 0; lnRow <= getDetailCount() - 1; lnRow++) {
                if (lnRow != row) {
                    if ((Detail(lnRow).getStockID().equals("") || Detail(lnRow).getStockID() == null)
                            || (Detail(lnRow).getStockID().equals(object.getModel().getStockId()))) {
                        poJSON.put("result", "error");
                        poJSON.put("message", "Barcode: " + object.getModel().getDescription() + " already exist in table at row " + (lnRow + 1) + ".");
                        poJSON.put("tableRow", lnRow);
                        return poJSON;
                    }
                }
            }
            
            Detail(row).setStockID(object.getModel().getStockId());
            Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
            if(row == getDetailCount() - 1){
                AddDetail();
            }
        }
        return poJSON;
    }
    
    public JSONObject SearchInventory(String value,int row, String Banks, boolean byCode) throws ExceptionInInitializerError, SQLException, GuanzonException {
        Inventory object = new InvControllers(poGRider, logwrapr).Inventory();
        object.setRecordStatus("1");

        poJSON = object.searchRecord(value, byCode);

        if ("success".equals((String) poJSON.get("result"))) {
           Detail(row).setStockID(object.getModel().getStockId());
           Detail(row).setUnitPrice(object.getModel().getCost().doubleValue());
           Detail(row).setClassify("F"); 
           
        }

        return poJSON;
    }
    
    @Override
    public void initSQL() {
        SQL_BROWSE = "SELECT "
                + " a.sTransNox, "
                + " a.dTransact, "
                + " c.sBranchNm, "
                + " a.sClientID, "
                + " d.sCompnyNm "
                + " FROM sales_reservation_master a "
                + " LEFT JOIN branch c ON LEFT(a.sTransNox, 4) = c.sBranchCd "
                + " LEFT JOIN client_master d ON a.sClientID = d.sClientID "
                + " LEFT JOIN client_address e ON d.sClientID = e.sClientID "
                + " LEFT JOIN client_mobile f ON d.sClientID = f.sClientID "                
                + ", sales_reservation_detail b ";
    }


    public JSONObject SearchTransaction(String fsValue) throws CloneNotSupportedException, SQLException, GuanzonException {
        poJSON = new JSONObject();
        String lsTransStat = "";
        String lsBranch = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
        } else {
            lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
        }

        initSQL();
        String lsFilterCondition = String.join(" AND ", "a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
                " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
                " a.sCategrCd LIKE " + SQLUtil.toSQL("%" + Master().getCategoryCode()));
        
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition);
        
        if (!fsValue.isEmpty()){
            if(Master().getClientID() == null){
                lsSQL = lsSQL +  " AND d.sCompnyNm LIKE " + SQLUtil.toSQL("%" + fsValue + "%");
            }else{
                lsSQL = lsSQL +  " AND a.sClientID = " + SQLUtil.toSQL( Master().getClientID());
            }
        }else{
         lsSQL = lsSQL +  " AND d.sCompnyNm LIKE " + SQLUtil.toSQL("%" + fsValue + "%");
        }
        
        
        
        if (!psTranStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }
        if (!poGRider.isMainOffice() || !poGRider.isWarehouse()) {
            lsSQL = lsSQL + " AND a.sBranchCd LIKE " + SQLUtil.toSQL(poGRider.getBranchCode());
        }

        lsSQL = lsSQL + " GROUP BY a.sTransNox";
        System.out.println("SQL EXECUTED: " + lsSQL);
        poJSON = ShowDialogFX.Browse(poGRider,
                lsSQL,
                fsValue,
                "Transaction Date»Transaction No»Customer Name»Branch",
                "a.dTransact»a.sTransNox»d.sCompnyNm»c.sBranchNm",
                "a.dTransact»a.sTransNox»d.sCompnyNm»ecsBranchNm",
                1);

        if (poJSON != null) {
            return OpenTransaction((String) poJSON.get("sTransNox"));
        } else {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "No record loaded.");
            return poJSON;
        }
    }
    
    /*End - Search Master References*/
    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public Model_Sales_Reservation_Master Master() {
        return (Model_Sales_Reservation_Master) poMaster;
    }

    @Override
    public Model_Sales_Reservation_Detail Detail(int row) {
        return (Model_Sales_Reservation_Detail) paDetail.get(row);
    }
    
    @Override
    public JSONObject willSave() throws SQLException, GuanzonException, CloneNotSupportedException {

        poJSON = new JSONObject();
        if (paDetailRemoved == null) {
            paDetailRemoved = new ArrayList<>();
        }
        Iterator<Model> detail = Detail().iterator();
        while (detail.hasNext()) {
            Model item = detail.next();
            Object quantityObj = item.getValue("nQuantity");
            Object stockIDObj = item.getValue("sStockIDx");

            // Check if the values are not null
            if (quantityObj != null && stockIDObj != null) {
                double quantity = ((Number) quantityObj).doubleValue();
                String stockID = (String) stockIDObj;
                
                if (stockID.isEmpty() || quantity <= 0.00) {
                    detail.remove();
                }
                
            } else {
                // Handle the case where the values are null
                detail.remove();
            }
        }
        
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
           Detail(lnCtr).setTransactionNo(Master().getTransactionNo());
           Detail(lnCtr).setEntryNo(lnCtr + 1);
           Detail(lnCtr).setModifiedDate(poGRider.getServerDate());
        }
       Master().setModifiedDate(poGRider.getServerDate());
       poJSON.put("result", "success");
       return poJSON;
    }

    

    @Override
    public JSONObject save() {
        /*Put saving business rules here*/
        return isEntryOkay(Sales_Reservation_Static.OPEN);
    }

    @Override
    public JSONObject saveOthers() {
        
        poJSON.put("result", "success");
        return poJSON;
    }

    @Override
    public void saveComplete() {
        /*This procedure was called when saving was complete*/
        System.out.println("Transaction saved successfully.");
    }

    
    public JSONObject validateDetails() {
        poJSON = new JSONObject();
        int detailCount = getDetailCount();
        if (detailCount == 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "Reservation cannot be saved without any detail. Please add an item.");
            return poJSON;
        }  
        for (int lnCtr = 0; lnCtr <= getDetailCount() - 1; lnCtr++) {
            if (detailCount == 1) {
                if (Detail(lnCtr).getStockID() == null || Detail(lnCtr).getStockID().isEmpty() || Detail(lnCtr).getQuantity() > 0.00) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Error: Please verify Stock ID");
                    return poJSON;
                }
            }
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    @Override
    protected JSONObject isEntryOkay(String status) {
        GValidator loValidator = Sales_Reservation_Validator_Factory.make(Master().getIndustryID());

        loValidator.setApplicationDriver(poGRider);
        loValidator.setTransactionStatus(status);
        loValidator.setMaster(Master());

        poJSON = loValidator.validate();
        return poJSON;
    }

    public JSONObject getUnifiedSource(String ClientID) throws SQLException, GuanzonException {
        StringBuilder lsSQL = new StringBuilder("SELECT * FROM (");
        boolean hasCondition = false;
        System.out.println("MASTER : " + psIndustryId + " " + Master().getIndustryID());
        
            if (hasCondition) {
                lsSQL.append(" UNION ALL ");
            }
            lsSQL.append(
                    "SELECT "
                    + " a.sTransNox, "
                    + " a.dTransact, "
                    + " 'Inquiry' AS source, "
                    + " a.sIndstCdx AS Industry, "    
                    + " a.sCompnyID AS Company "
                    + " FROM sales_inquiry_master a "
                    + " WHERE a.cTranStat = '" + Sales_Reservation_Static.CONFIRMED + "' "
                    + " AND a.cProcessd = '" + Sales_Reservation_Static.OPEN + "' "
                    + " AND a.sIndstCdx = '" + Master().getIndustryID() + "' "
                    + " AND a.sCompnyID = '" + Master().getCompanyID() + "'"
                    + " AND a.sClientID LIKE '" + (Master().getClientID() == null || Master().getClientID().isEmpty() ? "%" : Master().getClientID()) + "'"
            );
            hasCondition = true;
            
            if (hasCondition) {
                lsSQL.append(" UNION ALL ");
            }
            lsSQL.append(
                    "SELECT "
                    + " b.sTransNox, "
                    + " b.dTransact, "
                    + " 'Qoutation' AS source, "
                    + " b.sIndstCdx AS Industry, "
                    + " b.sCompnyID AS Company "
                    + " FROM sales_quotation_master b "
                    + " WHERE b.cTranStat = '" + Sales_Reservation_Static.CONFIRMED + "' "
                    + " AND b.sIndstCdx = '" + psIndustryId + "' "
                    + " AND b.sCompnyID = '" + Master().getCompanyID() + "'"
                    + " AND b.sClientID LIKE '" +  (Master().getClientID() == null || Master().getClientID().isEmpty() ? "%" : Master().getClientID()) + "'"
            );
            hasCondition = true;

        lsSQL.append(") AS CombinedResults ORDER BY dTransact ASC");

        System.out.println("Executing SQL: " + lsSQL.toString());

        ResultSet loRS = poGRider.executeQuery(lsSQL.toString());
        JSONArray dataArray = new JSONArray();
        JSONObject loJSON = new JSONObject();

        if (loRS == null) {
            loJSON.put("result", "error");
            loJSON.put("message", "Query execution failed.");
            return loJSON;
        }

        try {
            int lnctr = 0;

            while (loRS.next()) {
                JSONObject record = new JSONObject();
                record.put("sTransNox", loRS.getString("sTransNox"));
                record.put("dTransact", loRS.getDate("dTransact"));
                record.put("source", loRS.getString("source"));

                dataArray.add(record);
                lnctr++;
            }

            if (lnctr > 0) {
                loJSON.put("result", "success");
                loJSON.put("message", "Record(s) loaded successfully.");
                loJSON.put("data", dataArray);
            } else {
                loJSON.put("result", "error");
                loJSON.put("message", "No records found.");
                loJSON.put("data", new JSONArray());
            }

            MiscUtil.close(loRS);

        } catch (SQLException e) {
            loJSON.put("result", "error");
            loJSON.put("message", e.getMessage());
        }

        return loJSON;
    }

    public JSONObject addSourceToSalesRsvDetail(String transactionNo, String source)
            throws CloneNotSupportedException, SQLException, GuanzonException {

        poJSON = new JSONObject();
        int insertedCount = 0;
        int detailCount = 0;
        
        switch (source) {
            case Sales_Reservation_Static.Source.source_inquiry:
               SalesInquiry salesInquiry = new SalesControllers(poGRider, logwrapr).SalesInquiry();

                poJSON = salesInquiry.InitTransaction();
                if (!"success".equals(poJSON.get("result"))) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "No records found.");
                    return poJSON;
                }

                poJSON = salesInquiry.OpenTransaction(transactionNo);
                if (!"success".equals(poJSON.get("result"))) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "No records found.");
                    return poJSON;
                }
                
                detailCount = salesInquiry.getDetailCount();
//                String currentPayeeID = salesInquiry.Master().getPayeeID();

                for (int i = 0; i < detailCount; i++) {
                    if(salesInquiry.Detail(i).getStockId() == null || salesInquiry.Detail(i).getStockId().isEmpty()){
                        poJSON.put("result", "error");
                        poJSON.put("message", "Stock ID is not yet available");
                        return poJSON;
                    }
                    if (!Master().getSourceNo().isEmpty()){
                        if (!Master().getSourceNo().equals(salesInquiry.Master().getTransactionNo())){
                            poJSON.put("ischange", "true");
                             poJSON.put("result", "error");
                             poJSON.put("message", "Existing data will be cleared when adding a new inquiry or quotation. \n"
                                     + " Do you want to proceed?");
                             return poJSON;
                        }
                    }
                    if(salesInquiry.Detail(i).getStockId().equals(Detail(i).getStockID())){
                        poJSON.put("result", "error");
                        poJSON.put("message", "Stock ID is already exist in the detail");
                        return poJSON;
                    }
                    Master().setClientID(salesInquiry.Master().getClientId());
                    Master().setAddressID(salesInquiry.Master().getAddressId());
                    Master().setContactID(salesInquiry.Master().getContactId());
                    Master().setSourceNo(salesInquiry.Master().getTransactionNo());
                    Master().setSourceCode(salesInquiry.Master().getSourceCode());
                   
                    
                    AddDetail();
                    int newIndex = getDetailCount() - 1;
                    Detail(newIndex).setStockID(salesInquiry.Detail(i).getStockId());  
                    Detail(newIndex).setUnitPrice(salesInquiry.Detail(i).Inventory().getCost().doubleValue());  
                    Detail(newIndex).setMinimumDown(salesInquiry.Detail(i).Inventory().getCost().doubleValue());  
                    Detail(newIndex).setClassify(salesInquiry.Detail(i).InventoryMaster().getInventoryClassification());
                    insertedCount++;
                }
                break;
            default:
                poJSON.put("result", "error");
                poJSON.put("message", "Invalid source type.");
                return poJSON;
        }

        poJSON = new JSONObject();
        if (insertedCount == 0) {
            poJSON.put("result", "error");
            poJSON.put("message", "The selected transaction has already been inserted.");
        } else {
            poJSON.put("result", "success");
            poJSON.put("message", insertedCount + " detail(s) added successfully.");
        }

        return poJSON;
    }
    
    

    public JSONObject getReservationList(String fsTransactionNo, String fsCustomer) throws SQLException, GuanzonException {
        JSONObject loJSON = new JSONObject();
        String lsTransStat = "";
        if (psTranStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= psTranStat.length() - 1; lnCtr++) {
                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
            }
            lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
        } else {
            lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
        }

        initSQL();
        String lsFilterCondition = String.join(" AND ", "a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
                " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
                " a.sClientID LIKE " + SQLUtil.toSQL("%" + fsCustomer),
                " a.sTransNox  LIKE " + SQLUtil.toSQL("%" + fsTransactionNo),
                " a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()));
        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition);

        lsSQL = MiscUtil.addCondition(lsSQL, lsFilterCondition);
        if (!psTranStat.isEmpty()) {
            lsSQL = lsSQL + lsTransStat;
        }
        lsSQL = lsSQL + " GROUP BY  a.sTransNox"
                + " ORDER BY dTransact ASC";
        System.out.println("Executing SQL: " + lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        int lnCtr = 0;
        if (MiscUtil.RecordCount(loRS) >= 0) {
            poSalesReservationMaster = new ArrayList<>();
            while (loRS.next()) {
                // Print the result set
                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
                System.out.println("dTransact: " + loRS.getDate("dTransact"));
                System.out.println("------------------------------------------------------------------------------");

                poSalesReservationMaster.add(SalesReservationMasterList());
                poSalesReservationMaster.get(poSalesReservationMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
                lnCtr++;
            }
            System.out.println("Records found: " + lnCtr);
            loJSON.put("result", "success");
            loJSON.put("message", "Record loaded successfully.");
        } else {
            poSalesReservationMaster = new ArrayList<>();
            poSalesReservationMaster.add(SalesReservationMasterList());
            loJSON.put("result", "error");
            loJSON.put("continue", true);
            loJSON.put("message", "No record found .");
        }
        MiscUtil.close(loRS);
        return loJSON;
    }

    private Model_Sales_Reservation_Master SalesReservationMasterList() {
        return new SalesReservationModels(poGRider).Sales_Reservation_Master();
    }

    public int getSalesReservationCount() {
        return this.poSalesReservationMaster.size();
    }

    public Model_Sales_Reservation_Master poSalesReservationMasterList(int row) {
        return (Model_Sales_Reservation_Master) poSalesReservationMaster.get(row);
    }
    public void resetMaster() {
        poMaster = new SalesReservationModels(poGRider).Sales_Reservation_Master();
        Master().setIndustryID(psIndustryId);
        Master().setCompanyID(psCompanyId);
    }
    public void resetOthers() throws SQLException, GuanzonException {
//        checkPayments = new CashflowControllers(poGRider, logwrapr).CheckPayments();
//        Payees = new CashflowControllers(poGRider, logwrapr).Payee();
//        poPaymentRequest = new ArrayList<>();
//        poApPayments = new ArrayList<>();
//        poCachePayable = new ArrayList<>();
    }
}
