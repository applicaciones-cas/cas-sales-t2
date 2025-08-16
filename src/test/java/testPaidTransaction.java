
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import ph.com.guanzongroup.cas.sales.services.SalesReservationControllers;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testPaidTransaction {

    
    static GRiderCAS poApp;
    static SalesReservationControllers poSalesReservation;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/new/");

        poApp = MiscUtil.Connect();

        poSalesReservation = new SalesReservationControllers(poApp, null);
    }
    
    @Test
    public void testVerifyTransaction() {
        JSONObject loJSON;
        try {
            loJSON = poSalesReservation.SalesReservation().InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            loJSON = poSalesReservation.SalesReservation().OpenTransaction("M00125000001");
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }
            poApp.getDepartment();

            System.out.println("Transaction No: " + poSalesReservation.SalesReservation().Master().getTransactionNo());
            System.out.println("Transaction Date : " + poSalesReservation.SalesReservation().Master().getTransactionDate().toString());
            System.out.println("Branch: " + poSalesReservation.SalesReservation().Master().Branch().getBranchName());
            System.out.println("");
            int detailSize = poSalesReservation.SalesReservation().Detail().size();
            if (detailSize > 0) {
                 for (int lnCtr = 0; lnCtr < poSalesReservation.SalesReservation().Detail().size(); lnCtr++) {
                    System.out.println("DETAIL------------------- " + (lnCtr + 1));
                    System.out.println("TRANSACTION NO : " + poSalesReservation.SalesReservation().Master().getTransactionNo());
                    System.out.println("ENTRY No: " + poSalesReservation.SalesReservation().Detail(lnCtr).getEntryNo());
                    System.out.println("STOCKKID : " + poSalesReservation.SalesReservation().Detail(lnCtr).getStockID());
                    System.out.println("");
                 }
            }
            
            loJSON = poSalesReservation.SalesReservation().PaidTransaction("");
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }
        } catch (CloneNotSupportedException | SQLException | GuanzonException |ParseException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        }
    }
    @AfterClass
    public static void tearDownClass() {
        poSalesReservation = null;
        poApp = null;
    }
}
