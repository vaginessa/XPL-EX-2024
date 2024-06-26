package eu.faircode.xlua.randomizers;

import androidx.annotation.NonNull;

import eu.faircode.xlua.utilities.RandomStringGenerator;

public class RandomAdID implements IRandomizer {
    //google.advertisingid
    //Now its "ad.id"
    //84630630-u4ls-k487-f35f-h37afe0pomwq
    //00000000-0000-0000-0000-000000000000

    @Override
    public boolean isSetting(String setting) {
        return setting.equalsIgnoreCase(getSettingName());
    }

    @Override
    public String getSettingName() { return "ad.id"; }

    @Override
    public String getName() {
        return "Advertising ID";
    }

    @Override
    public String getID() {
        return "%ad_id%";
    }

    @Override
    public String generateString() {
        return RandomStringGenerator.generateRandomNumberString(8) +
                "-" +
                RandomStringGenerator.generateRandomAlphanumericString(4, RandomStringGenerator.LOWER_LETTERS) +
                "-" +
                RandomStringGenerator.generateRandomAlphanumericString(4, RandomStringGenerator.LOWER_LETTERS) +
                "-" +
                RandomStringGenerator.generateRandomAlphanumericString(4, RandomStringGenerator.LOWER_LETTERS) +
                "-" +
                RandomStringGenerator.generateRandomAlphanumericString(12, RandomStringGenerator.LOWER_LETTERS);
    }

    @Override
    public int generateInteger() { return 0; }

    @NonNull
    @Override
    public String toString() { return getName(); }
}
