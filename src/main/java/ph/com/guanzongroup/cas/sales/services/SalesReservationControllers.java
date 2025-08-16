package ph.com.guanzongroup.cas.sales.services;

import java.sql.SQLException;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.LogWrapper;
import ph.com.guanzongroup.cas.sales.t2.SalesReservation;

public class SalesReservationControllers {

    public SalesReservationControllers(GRiderCAS applicationDriver, LogWrapper logWrapper) {
        poGRider = applicationDriver;
        poLogWrapper = logWrapper;
    }

    public SalesReservation SalesReservation() throws SQLException, GuanzonException {
        if (poGRider == null) {
            poLogWrapper.severe("GLControllers.Disbursement: Application driver is not set.");
            return null;
        }

        if (poSalesReservation != null) {
            return poSalesReservation;
        }

        poSalesReservation = new SalesReservation();
        poSalesReservation.setApplicationDriver(poGRider);
        poSalesReservation.setBranchCode(poGRider.getBranchCode());
        poSalesReservation.setLogWrapper(poLogWrapper);
        poSalesReservation.setVerifyEntryNo(true);
        poSalesReservation.setWithParent(false);
        return poSalesReservation;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            
            poSalesReservation = null;
            poLogWrapper = null;
            poGRider = null;
        } finally {
            super.finalize();
        }
    }

    private GRiderCAS poGRider;
    private LogWrapper poLogWrapper;

   
    private SalesReservation poSalesReservation;
}
