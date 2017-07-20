package de.uni_freiburg.es.sensorrecordingtool.merger.retriever;


import java.io.File;
import java.util.Comparator;


/**
 * Compares two partial files against each others, picking the lowest of two. Partial files must
 * end with partXXX where XXX is the part number without leading zeros.
 */
public class PartialFileComperator implements Comparator<File> {
    @Override
    public int compare(File file1, File file2) {

        assert file1.getName().contains(".part");
        assert file2.getName().contains(".part");

        String file1PartString = file1.getName().substring(file1.getName().indexOf(".part") + 5);
        String file2PartString = file2.getName().substring(file1.getName().indexOf(".part") + 5);

        int file1part = Integer.parseInt(file1PartString);
        int file2part = Integer.parseInt(file2PartString);

        return Integer.compare(file1part, file2part);
    }
}
