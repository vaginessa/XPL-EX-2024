package eu.faircode.xlua.randomizers;

public interface IRandomizer {
    boolean isSetting(String settingName);
    String getSettingName();
    String getName();
    String getID();
    String generateString();
    int generateInteger();
    //byte[] generateBytes();
    //9177334325 jonny
}
