package ph.com.guanzongroup.cas.sales.services;

import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.LogWrapper;
import ph.com.guanzongroup.cas.sales.model.Model_Sales_Reservation_Detail;
import ph.com.guanzongroup.cas.sales.model.Model_Sales_Reservation_Master;

public class SalesReservationModels {
    public SalesReservationModels(GRiderCAS applicationDriver){
        poGRider = applicationDriver;
    }
    
    
    public Model_Sales_Reservation_Master Sales_Reservation_Master(){
        if (poGRider == null){
            System.err.println("CashflowModels.Sales Reservation Master: Application driver is not set.");
            return null;
        }
        
        if (poSalesReservationMaster == null){
            poSalesReservationMaster = new Model_Sales_Reservation_Master();
            poSalesReservationMaster.setApplicationDriver(poGRider);
            poSalesReservationMaster.setXML("Model_Sales_Reservation_Master");
            poSalesReservationMaster.setTableName("Sales_Reservation_Master");
            poSalesReservationMaster.initialize();
        }

        return poSalesReservationMaster;
    }

    public Model_Sales_Reservation_Detail Sales_Reservation_Detail(){
        if (poGRider == null){
            System.err.println("CashflowModels.Sales Reservation Detail: Application driver is not set.");
            return null;
        }
        
        if (poSalesReservationDetail == null){
            poSalesReservationDetail = new Model_Sales_Reservation_Detail();
            poSalesReservationDetail.setApplicationDriver(poGRider);
            poSalesReservationDetail.setXML("Model_Sales_Reservation_Detail");
            poSalesReservationDetail.setTableName("Sales_Reservation_Detail");
            poSalesReservationDetail.initialize();
        }

        return poSalesReservationDetail;
    }
    
    
    @Override
    protected void finalize() throws Throwable {
        try {                    
            poSalesReservationMaster = null;
            poSalesReservationDetail = null;
            
            poLogWrapper = null;
            poGRider = null;
        } finally {
            super.finalize();
        }
    }
    
    private GRiderCAS poGRider;
    
    private LogWrapper poLogWrapper;

   
    private Model_Sales_Reservation_Master poSalesReservationMaster;    
    private Model_Sales_Reservation_Detail poSalesReservationDetail; 
}