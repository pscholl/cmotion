package es.uni_freiburg.de.cmotion.model;


public class SensorModel implements Comparable<SensorModel>{

    private String mName;
    private boolean mEnabled;
    private int mSamplingRate = 50; // TODO use constant

    public SensorModel(String name) {
        this.mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
    }

    @Override
    public int compareTo(SensorModel another) {
        return getName().compareTo(another.getName());
    }

    public void setSamplingRate(int samplingRate) {
        this.mSamplingRate = samplingRate;
    }

    public int getSamplingRate() {
        return mSamplingRate;
    }
}
