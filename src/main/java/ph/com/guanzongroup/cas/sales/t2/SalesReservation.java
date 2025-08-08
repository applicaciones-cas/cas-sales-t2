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

    List<Model_Sales_Reservation_Master> poSalesReservatioNMaster;
  
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
            System.out.println("ADDRESS : " + object.ClientAddress().getModel().getAddressId());
            Master().setAddressID(object.ClientAddress().getModel().getAddressId());
            System.out.println("SETTED ADDRESS : " +  Master().getAddressID());
//            Master().setContactID(object.Mobile().getModel().getClientId());
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
                
                if (!stockID.isEmpty() && quantity == 0.00) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Quantity must be greater than 0 for the given stock.");
                    return poJSON;
                }
                // Remove only items with empty stock ID or zero quantity
                if (stockID.isEmpty() || quantity <= 0.00) {
                    detail.remove();
                }
                if (getDetailCount() == 0) {
                    poJSON.put("result", "error");
                    poJSON.put("message", "Reservation cannot be saved without any detail. Please add an item.");
                    return poJSON;
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

//    public JSONObject getReservation(String fsTransactionNo, String fsPayee, boolean isCheckPayment) throws SQLException, GuanzonException {
//        poJSON = new JSONObject();
//        // Build transaction status condition
//        String lsTransStat = "";
//        if (psTranStat.length() > 1) {
//            for (int lnCtr = 0; lnCtr < psTranStat.length(); lnCtr++) {
//                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
//            }
//            lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
//        } else if (!psTranStat.isEmpty()) {
//            lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
//        }
//
//        initSQL();
//
//        // Filter conditions (empty string means show all)
//        String lsFilterCondition = "a.sPayeeIDx LIKE " + SQLUtil.toSQL("%" + fsPayee + "%")
//                + " AND a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsTransactionNo + "%");
//
//        // Start from base SQL and apply filters
//        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition);
//
//        // Add transaction status condition
//        if (!lsTransStat.isEmpty()) {
//            lsSQL += lsTransStat;
//        }
//        if (isCheckPayment) {
//            lsSQL = lsSQL + " AND  a.cDisbrsTp = '0'";
//        }
//
//        // Grouping and sorting
//        lsSQL += " GROUP BY a.sTransNox ORDER BY a.dTransact ASC";
//
//        System.out.println("Executing SQL: " + lsSQL);
//
//        ResultSet loRS = poGRider.executeQuery(lsSQL);
//
//        int lnCtr = 0;
//        if (MiscUtil.RecordCount(loRS) >= 0) {
//            poDisbursementMaster = new ArrayList<>();
//            while (loRS.next()) {
//                // Print the result set
//                System.out.println("sTransNox: " + loRS.getString("sTransNox"));
//                System.out.println("dTransact: " + loRS.getDate("dTransact"));
//                System.out.println("------------------------------------------------------------------------------");
//
//                poDisbursementMaster.add(DisbursementMasterList());
//                poDisbursementMaster.get(poDisbursementMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
//                lnCtr++;
//            }
//            poJSON.put("result", "success");
//            poJSON.put("message", "Record loaded successfully.");
//        } else {
//            poDisbursementMaster = new ArrayList<>();
//            poDisbursementMaster.add(DisbursementMasterList());
//            poJSON.put("result", "error");
//            poJSON.put("continue", true);
//            poJSON.put("message", "No record found .");
//        }
//        MiscUtil.close(loRS);
//        return poJSON;
//    }
//
//    public JSONObject getDisbursementForCertification(String fsBankID, String fsBankAccountID) throws SQLException, GuanzonException {
//        poJSON = new JSONObject();
//        initSQL();
//        String lsFilterCondition = String.join(" AND ",
//                " a.cTranStat = " + SQLUtil.toSQL(DisbursementStatic.VERIFIED),
//                " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
//                " a.sCompnyID  = " + SQLUtil.toSQL(Master().getCompanyID()),
//                " i.sBankIDxx LIKE " + SQLUtil.toSQL("%" + fsBankID),
//                " j.sBnkActID LIKE " + SQLUtil.toSQL("%" + fsBankAccountID));
//
//        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition + " GROUP BY a.sTransNox ORDER BY a.dTransact ASC ");
//        System.out.println("Executing SQL: " + lsSQL);
//        ResultSet loRS = poGRider.executeQuery(lsSQL);
//        int lnCtr = 0;
//        if (MiscUtil.RecordCount(loRS)
//                >= 0) {
//            poDisbursementMaster = new ArrayList<>();
//            while (loRS.next()) {
//
//                poDisbursementMaster.add(DisbursementMasterList());
//                poDisbursementMaster.get(poDisbursementMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
//                lnCtr++;
//            }
//            poJSON.put("result", "success");
//            poJSON.put("message", "Record loaded successfully.");
//        } else {
//            poDisbursementMaster = new ArrayList<>();
//            poDisbursementMaster.add(DisbursementMasterList());
//            poJSON.put("result", "error");
//            poJSON.put("continue", true);
//            poJSON.put("message", "No record found .");
//        }
//
//        MiscUtil.close(loRS);
//        return poJSON;
//    }
//
//    public JSONObject getDisbursementForVerification(String fsRefNo, String fsSupplierPayee) throws SQLException, GuanzonException {
//        poJSON = new JSONObject();
//        String lsTransStat = "";
//        if (psTranStat.length() > 1) {
//            for (int lnCtr = 0; lnCtr < psTranStat.length(); lnCtr++) {
//                lsTransStat += ", " + SQLUtil.toSQL(Character.toString(psTranStat.charAt(lnCtr)));
//            }
//            lsTransStat = " AND a.cTranStat IN (" + lsTransStat.substring(2) + ")";
//        } else if (!psTranStat.isEmpty()) {
//            lsTransStat = " AND a.cTranStat = " + SQLUtil.toSQL(psTranStat);
//        }
//        initSQL();
//        String lsFilterCondition = String.join(" AND ",
//                //                " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
//                //                " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
//                " a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()),
//                " a.sTransNox LIKE " + SQLUtil.toSQL("%" + fsRefNo),
//                " a.sPayeeIDx LIKE " + SQLUtil.toSQL("%" + fsSupplierPayee));
//
//        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition);
//        if (!lsTransStat.isEmpty()) {
//            lsSQL += lsTransStat;
//        }
//        lsSQL += " GROUP BY a.sTransNox ORDER BY a.dTransact ASC ";
//
//        System.out.println("Executing SQL: " + lsSQL);
//        ResultSet loRS = poGRider.executeQuery(lsSQL);
//        int lnCtr = 0;
//        if (MiscUtil.RecordCount(loRS)
//                >= 0) {
//            poDisbursementMaster = new ArrayList<>();
//            while (loRS.next()) {
//                poDisbursementMaster.add(DisbursementMasterList());
//                poDisbursementMaster.get(poDisbursementMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
//                lnCtr++;
//            }
//            poJSON.put("result", "success");
//            poJSON.put("message", "Record loaded successfully.");
//        } else {
//            poDisbursementMaster = new ArrayList<>();
//            poDisbursementMaster.add(DisbursementMasterList());
//            poJSON.put("result", "error");
//            poJSON.put("continue", true);
//            poJSON.put("message", "No record found .");
//        }
//
//        MiscUtil.close(loRS);
//        return poJSON;
//    }
//
//    public JSONObject getDisbursementForCheckAuthorization(String fsBankID, String fsBankAccountID) throws SQLException, GuanzonException {
//        poJSON = new JSONObject();
//        initSQL();
//        String lsFilterCondition = String.join(" AND ",
//                " a.cTranStat = " + SQLUtil.toSQL(DisbursementStatic.CERTIFIED),
//                " a.sIndstCdx = " + SQLUtil.toSQL(Master().getIndustryID()),
//                " a.sCompnyID = " + SQLUtil.toSQL(Master().getCompanyID()),
//                " i.sBankIDxx LIKE " + SQLUtil.toSQL("%" + fsBankID),
//                " j.sBnkActID LIKE " + SQLUtil.toSQL("%" + fsBankAccountID));
//
//        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition + " GROUP BY a.sTransNox ORDER BY a.dTransact ASC ");
//        System.out.println("Executing SQL: " + lsSQL);
//        ResultSet loRS = poGRider.executeQuery(lsSQL);
//        int lnCtr = 0;
//        if (MiscUtil.RecordCount(loRS)
//                >= 0) {
//            poDisbursementMaster = new ArrayList<>();
//            while (loRS.next()) {
//                poDisbursementMaster.add(DisbursementMasterList());
//                poDisbursementMaster.get(poDisbursementMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
//                lnCtr++;
//            }
//            poJSON.put("result", "success");
//            poJSON.put("message", "Record loaded successfully.");
//        } else {
//            poDisbursementMaster = new ArrayList<>();
//            poDisbursementMaster.add(DisbursementMasterList());
//            poJSON.put("result", "error");
//            poJSON.put("continue", true);
//            poJSON.put("message", "No record found .");
//        }
//
//        MiscUtil.close(loRS);
//        return poJSON;
//    }
//
//    public JSONObject getDisbursementForCheckStatusUpdate(String fsBankID, String fsBankAccountID, String fsCheckNo) throws SQLException, GuanzonException {
//        poJSON = new JSONObject();
//        initSQL();
//        String lsFilterCondition = String.join(" AND ",
//                " a.cDisbrsTp = " + SQLUtil.toSQL(Logical.NO),
//                " g.sBankIDxx LIKE " + SQLUtil.toSQL("%" + fsBankID),
//                " g.sBnkActID LIKE " + SQLUtil.toSQL("%" + fsBankAccountID),
//                " g.sCheckNox LIKE " + SQLUtil.toSQL("%" + fsCheckNo));
////                " g.cTranStat IN ('1', '5')");
//        String lsSQL = MiscUtil.addCondition(SQL_BROWSE, lsFilterCondition + " GROUP BY a.sTransNox ORDER BY a.dTransact ASC ");
//        System.out.println("Executing SQL: " + lsSQL);
//        ResultSet loRS = poGRider.executeQuery(lsSQL);
//
//        int lnCtr = 0;
//
//        if (MiscUtil.RecordCount(loRS)
//                >= 0) {
//            poDisbursementMaster = new ArrayList<>();
//            while (loRS.next()) {
//                poDisbursementMaster.add(DisbursementMasterList());
//                poDisbursementMaster.get(poDisbursementMaster.size() - 1).openRecord(loRS.getString("sTransNox"));
//                lnCtr++;
//            }
//            poJSON.put("result", "success");
//            poJSON.put("message", "Record loaded successfully.");
//        } else {
//            poDisbursementMaster = new ArrayList<>();
//            poDisbursementMaster.add(DisbursementMasterList());
//            poJSON.put("result", "error");
//            poJSON.put("continue", true);
//            poJSON.put("message", "No record found .");
//        }
//
//        MiscUtil.close(loRS);
//        return poJSON;
//    }
//
//    private Model_Sales_Reservation_Master DisbursementMasterList() {
//        return new CashflowModels(poGRider).DisbursementMaster();
//    }
//
//    public int getDisbursementMasterCount() {
//        return this.poDisbursementMaster.size();
//    }
//
//    public Model_Sales_Reservation_Master poDisbursementMaster(int row) {
//        return (Model_Sales_Reservation_Master) poDisbursementMaster.get(row);
//    }
//
//    public JSONObject computeFields() {
//        poJSON = new JSONObject();
//
//        double lnTotalVatSales = 0.0000;         // Vatable Sales
//        double VAT_RATE = 0.12;                  // 12% VAT
//        double lnTotalVatAmount = 0.0000;        // VAT Amount
//        double lnTotalPurchaseAmount = 0.0000;   // Gross Purchased Amount
//        double lnLessWithHoldingTax = 0.0000;    // Withholding Tax
//        double lnTotalVatExemptSales = 0.0000;   // VAT EXEMPT
//
//        boolean hasVat = false; // 👉 Flag to check if at least one detail has VAT
//
//        for (int lnCntr = 0; lnCntr <= getDetailCount() - 1; lnCntr++) {
//            double detailAmount = Detail(lnCntr).getAmount();
//            double detailTaxRate = Detail(lnCntr).getTaxRates();
//
//            Detail(lnCntr).setTaxRates(Detail(lnCntr).getTaxRates());
//            Detail(lnCntr).setTaxAmount(detailAmount * Detail(lnCntr).getTaxRates() / 100);
//
//            lnTotalPurchaseAmount += detailAmount;
//
//            // Withholding Tax Computation
//            lnLessWithHoldingTax += detailAmount * (detailTaxRate / 100);
//
//            if (Detail(lnCntr).isWithVat()) {
//                hasVat = true; // 👉 At least one VAT item found
//
//                double lnVatableSales = detailAmount / (1 + VAT_RATE);
//                double lnVatAmount = detailAmount - lnVatableSales;
//
//                lnTotalVatSales += lnVatableSales;
//                lnTotalVatAmount += lnVatAmount;
//            } else {
//                lnTotalVatExemptSales += detailAmount;
//            }
//        }
//
//        // ✅ Set VAT rate based on whether VAT exists
//        if (hasVat) {
//            Master().setVATRates(VAT_RATE * 100);
//        } else {
//            Master().setVATRates(0.00);
//        }
//
//        double lnNetAmountDue = lnTotalPurchaseAmount - lnLessWithHoldingTax;
//
//        if (lnNetAmountDue < 0.0000) {
//            poJSON.put("result", "error");
//            poJSON.put("message", "Invalid Net Total Amount.");
//            return poJSON;
//        }
//
//        // Save Computed Values
//        Master().setTransactionTotal(lnTotalPurchaseAmount);
//        Master().setVATSale(lnTotalVatSales);
//        Master().setVATAmount(lnTotalVatAmount);
//        Master().setVATExmpt(lnTotalVatExemptSales);
//        Master().setZeroVATSales(0.00);
//        Master().setWithTaxTotal(lnLessWithHoldingTax);
//        Master().setNetTotal(lnNetAmountDue);
//
//        if (Master().getDisbursementType().equals(DisbursementStatic.DisbursementType.CHECK)) {
//            checkPayments.getModel().setAmount(Master().getNetTotal());
//        }
//
//        poJSON.put("result", "success");
//        poJSON.put("message", "computed successfully");
//        return poJSON;
//    }
//
//    public void exportDisbursementMasterMetadataToXML(String filePath) throws SQLException, IOException {
//        String query = "SELECT "
//                + "  sTransNox, "
//                + "  sBranchCd, "
//                + "  sIndstCdx, "
//                + "  dTransact, "
//                + "  sBankIDxx, "
//                + "  sRemarksx, "
//                + "  nEntryNox, "
//                + "  nTotalAmt, "
//                + "  cIsUpload, "
//                + "  cTranStat, "
//                + "  sModified, "
//                + "  dModified "
//                + "FROM check_printing_master";
//
//        ResultSet rs = poGRider.executeQuery(query);
//
//        if (rs == null) {
//            throw new SQLException("Failed to execute query.");
//        }
//
//        ResultSetMetaData metaData = rs.getMetaData();
//        int columnCount = metaData.getColumnCount();
//
//        StringBuilder xml = new StringBuilder();
//        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
//        xml.append("<metadata>\n");
//        xml.append("  <table>Check_Printing_Master</table>\n");
//
//        for (int i = 1; i <= columnCount; i++) {
//            xml.append("  <column>\n");
//            xml.append("    <COLUMN_NAME>").append(metaData.getColumnName(i)).append("</COLUMN_NAME>\n");
//            xml.append("    <COLUMN_LABEL>").append(metaData.getColumnLabel(i)).append("</COLUMN_LABEL>\n");
//            xml.append("    <DATA_TYPE>").append(metaData.getColumnType(i)).append("</DATA_TYPE>\n");
//            xml.append("    <NULLABLE>").append(metaData.isNullable(i) == ResultSetMetaData.columnNullable ? 1 : 0).append("</NULLABLE>\n");
//            xml.append("    <LENGTH>").append(metaData.getColumnDisplaySize(i)).append("</LENGTH>\n");
//            xml.append("    <PRECISION>").append(metaData.getPrecision(i)).append("</PRECISION>\n");
//            xml.append("    <SCALE>").append(metaData.getScale(i)).append("</SCALE>\n");
//            xml.append("    <FORMAT>null</FORMAT>\n");
//            xml.append("    <REGTYPE>null</REGTYPE>\n");
//            xml.append("    <FROM>null</FROM>\n");
//            xml.append("    <THRU>null</THRU>\n");
//            xml.append("    <LIST>null</LIST>\n");
//            xml.append("  </column>\n");
//        }
//
//        xml.append("</metadata>");
//
//        try (FileWriter writer = new FileWriter(filePath)) {
//            writer.write(xml.toString());
//        }
//
//        MiscUtil.close(rs);
//    }
//
//    public void resetMaster() {
//        poMaster = new CashflowModels(poGRider).DisbursementMaster();
//        Master().setIndustryID(psIndustryId);
//        Master().setCompanyID(psCompanyId);
//    }
//
//    public void resetJournal() {
//        try {
//            poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
//            poJournal.InitTransaction();
//        } catch (SQLException | GuanzonException ex) {
//            Logger.getLogger(SalesReservation.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//
//    public Journal Journal() throws SQLException, GuanzonException {
//        if (poJournal == null) {
//            poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
//            poJournal.InitTransaction();
//        }
//        return poJournal;
//    }

//    public JSONObject populateJournal() throws SQLException, GuanzonException, CloneNotSupportedException, ScriptException {
//        poJSON = new JSONObject();
//        if (poJournal == null || getEditMode() == EditMode.READY) {
//            poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
//            poJournal.InitTransaction();
//        }
//        String lsJournal = existJournal();
//        switch (getEditMode()) {
//            case EditMode.UNKNOWN:
//                poJSON.put("result", "error");
//                poJSON.put("message", "No record to load");
//                return poJSON;
//            case EditMode.ADDNEW:
//                if (poJournal.Master() != null && poJournal.Master().getTransactionNo() != null) {
//                    // Transaction already exists, just skip creating new one
//                    break;
//                }
//                poJournal = new CashflowControllers(poGRider, logwrapr).Journal();
//                poJournal.InitTransaction();
//                poJSON = poJournal.NewTransaction();
//                if ("error".equals((String) poJSON.get("result"))) {
//                    return poJSON;
//                }
//
////                double ldblNetTotal = 0.0000;
////                double ldblDiscount = Master().getDiscount().doubleValue();
////                double ldblDiscountRate = Master().getDiscountRate().doubleValue();
////                if (ldblDiscountRate > 0) {
////                    ldblDiscountRate = Master().getTransactionTotal().doubleValue() * (ldblDiscountRate / 100);
////                }
////                ldblDiscount = ldblDiscount + ldblDiscountRate;
////                //Net Total = Vat Amount - Tax Amount
////                if (Master().isVatTaxable()) {
////                    //Net VAT Amount : VAT Sales - VAT Amount
////                    //Net Total : VAT Sales - Withholding Tax
////                    ldblNetTotal = Master().getVatSales().doubleValue() - Master().getWithHoldingTax().doubleValue();
////                } else {
////                    //Net VAT Amount : VAT Sales + VAT Amount
////                    //Net Total : Net VAT Amount - Withholding Tax
////                    ldblNetTotal = (Master().getVatSales().doubleValue()
////                            + Master().getVatAmount().doubleValue())
////                            - Master().getWithHoldingTax().doubleValue();
////
////                }
////                JSONObject jsonmaster = new JSONObject();
////                jsonmaster.put("nWTaxTotl", Master().getWithTaxTotal());
////                jsonmaster.put("nDiscTotl", Master().getDiscountTotal());
////                jsonmaster.put("nNetTotal", Master().getNetTotal());
////                jsonmaster.put("cPaymType", "0");
////
////                JSONArray jsondetails = new JSONArray();
////
////                JSONObject jsondetail = new JSONObject();
////                jsondetail.put("sAcctCode", "2101010");
////                jsondetail.put("nAmtAppld", Master().getNetTotal());
////
////                jsondetails.add(jsondetail);
////
////                jsondetail = new JSONObject();
////                jsondetail.put("sAcctCode", "5201000");
////                jsondetail.put("nAmtAppld", Master().getNetTotal());
////                jsondetails.add(jsondetail);
////
////                jsondetail = new JSONObject();
////                jsondetail.put("Disbursement_Master", jsonmaster);
////                jsondetail.put("Disbursement_Detail", jsondetails);
//                System.out.println("MASTER");
//                //retreiving using column index
//                JSONObject jsonmaster = new JSONObject();
//                for (int lnCtr = 1; lnCtr <= Master().getColumnCount(); lnCtr++) {
//                    System.out.println(Master().getColumn(lnCtr) + " ->> " + Master().getValue(lnCtr));
//                    jsonmaster.put(Master().getColumn(lnCtr), Master().getValue(lnCtr));
//                }
//
//                JSONArray jsondetails = new JSONArray();
//                JSONObject jsondetail = new JSONObject();
//
//                System.out.println("DETAIL");
//                for (int lnCtr = 0; lnCtr <= Detail().size() - 1; lnCtr++) {
//                    jsondetail = new JSONObject();
//                    System.out.println("DETAIL ROW : " + lnCtr);
//                    for (int lnCol = 1; lnCol <= Detail(lnCtr).getColumnCount(); lnCol++) {
//                        System.out.println(Detail(lnCtr).getColumn(lnCol) + " ->> " + Detail(lnCtr).getValue(lnCol));
//                        jsondetail.put(Detail(lnCtr).getColumn(lnCol), Detail(lnCtr).getValue(lnCol));
//                    }
//                    jsondetails.add(jsondetail);
//                }
//                jsondetail = new JSONObject();
//                jsondetail.put("Disbursement_Master", jsonmaster);
//                jsondetail.put("Disbursement_Detail", jsondetails);
//
//                TBJTransaction tbj = new TBJTransaction(SOURCE_CODE, Master().getIndustryID(), "");
//                tbj.setGRiderCAS(poGRider);
//                tbj.setData(jsondetail);
//                jsonmaster = tbj.processRequest();
//
//                if (jsonmaster.get("result").toString().equalsIgnoreCase("success")) {
//                    List<TBJEntry> xlist = tbj.getJournalEntries();
//                    for (TBJEntry xlist1 : xlist) {
//                        System.out.println("Account:" + xlist1.getAccount());
//                        System.out.println("Debit:" + xlist1.getDebit());
//                        System.out.println("Credit:" + xlist1.getCredit());
//                        poJournal.Detail(poJournal.getDetailCount() - 1).setForMonthOf(poGRider.getServerDate());
//                        poJournal.Detail(poJournal.getDetailCount() - 1).setAccountCode(xlist1.getAccount());
//                        poJournal.Detail(poJournal.getDetailCount() - 1).setCreditAmount(xlist1.getCredit());
//                        poJournal.Detail(poJournal.getDetailCount() - 1).setDebitAmount(xlist1.getDebit());
//                        poJournal.AddDetail();
//                    }
//                } else {
//                    System.out.println(jsonmaster.toJSONString());
//                }
//                // Build Master
//                poJournal.Master().setAccountPerId("dummy");
//                poJournal.Master().setIndustryCode(Master().getIndustryID());
//                poJournal.Master().setBranchCode(Master().getBranchCode());
//                poJournal.Master().setDepartmentId(poGRider.getDepartment());
//                poJournal.Master().setTransactionDate(poGRider.getServerDate());
//                poJournal.Master().setCompanyId(Master().getCompanyID());
//                poJournal.Master().setSourceCode(getSourceCode());
//                poJournal.Master().setSourceNo(Master().getTransactionNo());
//                break;
//            case EditMode.UPDATE:
//                if (lsJournal != null && !"".equals(lsJournal)) {
//                    Journal().UpdateTransaction();
//                }
//                break;
//            case EditMode.READY:
//                if (lsJournal != null && !"".equals(lsJournal)) {
//                    poJSON = poJournal.OpenTransaction(lsJournal);
//                    System.out.println(poJSON.clone());
//                    if ("error".equals((String) poJSON.get("result"))) {
//                        return poJSON;
//                    }
//                }
//                break;
//            default:
//                poJSON.put("result", "error");
//                poJSON.put("message", "No record to load");
//                return poJSON;
//        }
//        poJSON.put("result", "success");
//        return poJSON;
//    }
    
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

//   @Override
//    public JSONObject initFields() {
//        //Put initial model values here/
//        poJSON = new JSONObject();
//        try {
//            poJSON = new JSONObject();
//            Master().setBranchCode(poGRider.getBranchCode());
//            Master().setIndustryID(psIndustryId);
//            Master().setCompanyID(psCompanyId);
//            Master().setTransactionDate(poGRider.getServerDate());
//            Master().setTransactionStatus(DisbursementStatic.OPEN);
//
//        } catch (SQLException ex) {
//            Logger.getLogger(SalesReservation.class
//                    .getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
//            poJSON.put("result", "error");
//            poJSON.put("message", MiscUtil.getException(ex));
//            return poJSON;
//        }
//        poJSON.put("result", "success");
//        return poJSON;
//    }
    
//
//    public String getVoucherNo() throws SQLException {
//        String lsSQL = "SELECT sVouchrNo FROM disbursement_master";
//        lsSQL = MiscUtil.addCondition(lsSQL,
//                "sBranchCd = " + SQLUtil.toSQL(Master().getBranchCode())
//                + " ORDER BY sVouchrNo DESC LIMIT 1");
//
//        String branchVoucherNo = DisbursementStatic.DEFAULT_VOUCHER_NO;  // default value
//
//        ResultSet loRS = null;
//        try {
//            System.out.println("EXECUTING SQL :  " + lsSQL);
//            loRS = poGRider.executeQuery(lsSQL);
//
//            if (loRS != null && loRS.next()) {
//                String sSeries = loRS.getString("sVouchrNo");
//                if (sSeries != null && !sSeries.trim().isEmpty()) {
//                    long voucherNumber = Long.parseLong(sSeries);
//                    voucherNumber += 1;
//                    branchVoucherNo = String.format("%08d", voucherNumber); // format to 6 digits
//                }
//            }
//        } finally {
//            MiscUtil.close(loRS);  // Always close the ResultSet
//        }
//        return branchVoucherNo;
//    }
//
//   

//    private static String xsDateShort(Date fdValue) {
//        if (fdValue == null) {
//            return "1900-01-01";
//        }
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        String date = sdf.format(fdValue);
//        return date;
//    }
}
