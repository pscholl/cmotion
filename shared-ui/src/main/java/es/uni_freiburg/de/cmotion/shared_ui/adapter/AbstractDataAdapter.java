package es.uni_freiburg.de.cmotion.shared_ui.adapter;

import java.util.List;

 public interface AbstractDataAdapter<T> {
     void setData(List<T> collection);
}
