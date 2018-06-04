package services;

import java.util.Comparator;

public class CompararPorOrden implements Comparator<Mensaje>{

	@Override
	public int compare(Mensaje m1, Mensaje m2) {
		float orden1 = m1.orden;
		float orden2 = m2.orden;
		
		if(orden1 < orden2) {
			return -1;
		} else if(orden1 > orden2) {
			return 1;
		} else {
			return 0;
		}
	}

}
