//import OrderManager.Fill;
//import OrderManager.Order;
//import Ref.Instrument;
//import Ref.Ric;
//import org.testng.annotations.Test;
//
//
//
//
//import static org.testng.AssertJUnit.assertEquals;
//import static org.testng.AssertJUnit.assertFalse;
//
//
//public class UnitTestsForOrders {
//
//    /*** in Order; size, slices and fills need to be made public to do these tests */
//
//    private Order testOrders = new Order(1,1, new Instrument(new Ric("TEST.TEST")), 10);
//
//
//    public String toString(Order order){
//        return ("Client ID " + order.id + " Client orderID " + order.clientOrderID + " size " + order.size);
//    }
//
//
//    @Test
//    public void testSlicing(){
//
//        int sliceSize = testOrders.newSlice(2);
//        int sliceSize2 = testOrders.newSlice(4);
//
//
//        System.out.println("size of order " + testOrders.size);
//
//        for (int i = 0; i < testOrders.slices.size(); i++){
//            System.out.println(toString(testOrders.slices.get(i)));
//        }
//
//        // The arraylist of slices only contains the slices and does NOT contain the original order as well
//        assertEquals(2, testOrders.slices.size());
//
//    }
//
//    // Tests if the total number of sliced orders is correct.
//    @Test
//    public void testSlicingSizes(){
//
//        // If all tests are run at once this makes sure we don't create duplicate slices
//        if (testOrders.slices.size() == 0) {
//            int sliceSize = testOrders.newSlice(2);
//            int sliceSize2 = testOrders.newSlice(4);
//        }
//
//        int totalSliceSizes = testOrders.sliceSizes();
//
//        assertEquals(6, totalSliceSizes);
//    }
//
//    // Tests if the total number of slices is bigger than the original size of the order. There are no size limits
//    // on the size of sliced orders
//    @Test
//    public void testSlicingSizesExceedSize(){
//
//        int sliceSize = testOrders.newSlice(2);
//        int sliceSize2 = testOrders.newSlice(4);
//
//        int totalSliceSizes = testOrders.sliceSizes();
//
//        assertEquals(true, totalSliceSizes > testOrders.size);
//    }
//
//    // Tests both if original fill is zero and then if a fill is added if it updates
//    @Test
//    public void testSizeFilled(){
//
//        int filledSoFar = testOrders.sizeFilled();
//
//        assertEquals(0, filledSoFar );
//
//        if (testOrders.fills.isEmpty()) {
//            testOrders.createFill(2, 200D);
//        }
//
//        filledSoFar = testOrders.sizeFilled();
//        assertEquals(2, filledSoFar);
//    }
//
//    // This test fails because it only takes into account the number of fills but not the actual size of the fill.
//    @Test
//    public void testPrice(){
//
//        if (testOrders.fills.isEmpty()){
//            testOrders.createFill(1, 200D);
//        }
//
//        testOrders.createFill(2, 100D);
//
//        float averagePrice = testOrders.price();
//
//        assertEquals(100F, averagePrice);
//
//    }
//
//
//
//    // Tests if orders that do match both get filled properly.
//    @Test
//    public void testCrossInOrderWithMatching(){
//
//        Order matchTestOrder = new Order(2,1, new Instrument(new Ric("TEST.TEST")), 5);
//
//        if (testOrders.slices.size() == 0) {
//            int sliceSize = testOrders.newSlice(2);
//            int sliceSize2 = testOrders.newSlice(4);
//        }
//
//        if (matchTestOrder.slices.size() == 0) {
//            int sliceSize = matchTestOrder.newSlice(2);
//            int sliceSize2 = matchTestOrder.newSlice(3);
//        }
//
//        testOrders.cross(matchTestOrder);
//
//        System.out.println("test size: " + testOrders.size);
//        System.out.println("matchtest size: " + matchTestOrder.size);
//
//        System.out.println("test size remaining: " + testOrders.sizeRemaining());
//        System.out.println("matchtest size remaining: " + matchTestOrder.sizeRemaining());
//
//        System.out.println("test size filled: " + testOrders.sizeFilled());
//        System.out.println("matchtest size filled: " + matchTestOrder.sizeFilled());
//
//        assertEquals(testOrders.sizeFilled(), matchTestOrder.sizeFilled());
//    }
//
//    // Tests if orders that do NOT match both get filled properly.
//    // THIS TEST FAILS AS THE order.cross does NOT check if they are the same!
//    // That is though ok because it is checked before it goes into order.cross in OrderManagers internal cross
//    @Test
//    public void testCrossInOrderWithNOTMatching(){
//
//        Order notMatchTestOrder = new Order(2,1, new Instrument(new Ric("TEST.NOT")), 5);
//
//        if (testOrders.slices.size() == 0) {
//            int sliceSize = testOrders.newSlice(2);
//            int sliceSize2 = testOrders.newSlice(4);
//        }
//
//        if (notMatchTestOrder.slices.size() == 0) {
//            int sliceSize = notMatchTestOrder.newSlice(2);
//            int sliceSize2 = notMatchTestOrder.newSlice(3);
//        }
//
//        testOrders.cross(notMatchTestOrder);
//
//        System.out.println("test size: " + testOrders.size);
//        System.out.println("matchtest size: " + notMatchTestOrder.size);
//
//        System.out.println("test size remaining: " + testOrders.sizeRemaining());
//        System.out.println("matchtest size remaining: " + notMatchTestOrder.sizeRemaining());
//
//        System.out.println("test size filled: " + testOrders.sizeFilled());
//        System.out.println("matchtest size filled: " + notMatchTestOrder.sizeFilled());
//
//        assertFalse(testOrders.sizeFilled() == notMatchTestOrder.sizeFilled());
//    }
//
//    //TODO: MAKE TEST FOR CANCEL WHEN IT IS READY
//
//
//}
