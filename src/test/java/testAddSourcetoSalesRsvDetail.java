
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import ph.com.guanzongroup.cas.sales.services.SalesReservationControllers;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testAddSourcetoSalesRsvDetail {

    static GRiderCAS poApp;
    static SalesReservationControllers poSalesReservation;

    @BeforeClass
    public static void setUpClass() {

        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/new/");

        poApp = MiscUtil.Connect();

        poSalesReservation = new SalesReservationControllers(poApp, null);

    }

    @Test
    public void testOpenTransaction() {
        JSONObject loJSON;
        String transactionNo = "M00125000018";
        String source = "Inquiry";

        try {
            loJSON = poSalesReservation.SalesReservation().InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            loJSON = poSalesReservation.SalesReservation().NewTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }
            loJSON = poSalesReservation.SalesReservation().addSourceToSalesRsvDetail(transactionNo, source);
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }
            
            for (int i = 0; i < poSalesReservation.SalesReservation().getDetailCount(); i++) {
                System.out.println("Master ");
                System.out.println("  ClientID   : " + poSalesReservation.SalesReservation().Master().getClientID());
                System.out.println("Detail #" + (i + 1));
                System.out.println("  StockID   : " + poSalesReservation.SalesReservation().Detail(i).getStockID());
//                System.out.println("  particular : " + poSalesReservation.SalesReservation().Detail(i).getParticularID());
//                System.out.println("  Amount      : " + poSalesReservation.SalesReservation().Detail(i).getAmount());
//                System.out.println("  InvType      : " + poSalesReservation.SalesReservation().Detail(i).getInvType());
                System.out.println("------------------------------------");
            }
            
        } catch (SQLException | GuanzonException | CloneNotSupportedException e) {
            Logger.getLogger(MiscUtil.getException(e));
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
