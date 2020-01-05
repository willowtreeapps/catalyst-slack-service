package services;

public class MockCorrector implements MessageCorrector {

    @Override
    public String getCorrection(String input) {
        return (input.equals("she's so quiet")) ? "she's so thoughtful" : "";
    }
}
