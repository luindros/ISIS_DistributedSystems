package services;

public class Mensaje {
	
	//ATRIBUTOS
	String contenido;
	String id_mensaje;
	float orden;
	String estado;
	String emisor;
	int numPropuestasRecibidas;
	
	//CONSTRUCTOR
	public Mensaje(String contenido_, String id_mensaje_, String estado_, String emisor_){
		contenido = contenido_;
		id_mensaje = id_mensaje_;
		estado = estado_;
		emisor = emisor_;
		numPropuestasRecibidas = 0;
	}
	
}
