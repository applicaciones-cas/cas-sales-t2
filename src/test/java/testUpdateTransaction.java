
import java.sql.SQLException;
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
public class testUpdateTransaction {

    static GRiderCAS poApp;
    static SalesReservationControllers poSalesReservation;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/new/");

        poApp = MiscUtil.Connect();

        poSalesReservation = new SalesReservationControllers(poApp, null);
    }

    @Test
    public void testUpdateTransaction() throws GuanzonException {
        JSONObject loJSON;

        try {
            loJSON = (JSONObject) poSalesReservation.SalesReservation().InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            loJSON = (JSONObject) poSalesReservation.SalesReservation().OpenTransaction("M00125000005");
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            loJSON = (JSONObject) poSalesReservation.SalesReservation().UpdateTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

//           poSalesReservation.SalesReservation().Detail(1).setParticularID("M001250002");
//           poSalesReservation.SalesReservation().Detail(2).setParticularID("M001250003");
//           poSalesReservation.SalesReservation().Detail(0).setAmount(1000);
//           poSalesReservation.SalesReservation().Detail(1).setAmount(2000);
//           poSalesReservation.SalesReservation().Detail(2).setAmount(3000);
//
//           poSalesReservation.SalesReservation().Detail(0).setModifiedDate(poApp.getServerDate());
//           poSalesReservation.SalesReservation().Detail(1).setModifiedDate(poApp.getServerDate());
//           poSalesReservation.SalesReservation().Detail(2).setModifiedDate(poApp.getServerDate());

            loJSON =poSalesReservation.SalesReservation().Detail(0).setQuantity(0.00);
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }
             
             
                    poSalesReservation.SalesReservation().Detail(1).setStockID("M00125000004");
                        Assert.assertEquals(poSalesReservation.SalesReservation().Detail(0).getStockID(), "M00125000004"); 
                    poSalesReservation.SalesReservation().Detail(1).setQuantity(1.00);
//                        Assert.assertEquals( poSalesReservation.SalesReservation().Detail(0).getQuantity(), quantity); 
                    poSalesReservation.SalesReservation().Detail(1).setUnitPrice(10000.0000);
//                        Assert.assertEquals(10000.0000, poSalesReservation.SalesReservation().Detail(0).getUnitPrice()); 
                    poSalesReservation.SalesReservation().Detail(1).setMinimumDown(1000.0000);
//                        Assert.assertEquals(1000.0000, poSalesReservation.SalesReservation().Detail(0).getMinimumDown()); 
                    poSalesReservation.SalesReservation().Detail(1).setClassify("F");
//                        Assert.assertEquals("F", poSalesReservation.SalesReservation().Detail(0).getClassify()); 
                    poSalesReservation.SalesReservation().Detail(1).setApproved(0);
//                        Assert.assertEquals(0, poSalesReservation.SalesReservation().Detail(0).getMinimumDown()); 
                    poSalesReservation.SalesReservation().Detail(1).setNotes("sam");
                        Assert.assertEquals(poSalesReservation.SalesReservation().Detail(0).getNotes(), "sam");
            poSalesReservation.SalesReservation().AddDetail();
            
           

            loJSON =poSalesReservation.SalesReservation().SaveTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }
        } catch (CloneNotSupportedException | SQLException e) {
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
