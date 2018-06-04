package services;

import java.util.HashMap;
import java.util.concurrent.Semaphore;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.QueryParam;

@Path("dispatcher")
@Singleton
public class Dispatcher {
	
	//ATRIBUTOS
	
	HashMap<String,Proceso> procesos = new HashMap<String,Proceso>();
	
	Semaphore semFinalizacion = new Semaphore(0,true);
	
	Semaphore semAtomicoPreparados = new Semaphore(1,true);
	Semaphore semPreparados = new Semaphore(0,true);
	public int numPreparados=0;
	
	//SERVICIOS
	
	@Path("reinicio")
	@GET 
	@Produces(MediaType.TEXT_PLAIN)
	public String reinicio() {
		
		procesos = new HashMap<String,Proceso>();
		
		semFinalizacion = new Semaphore(0,true);
		
		semAtomicoPreparados = new Semaphore(1,true);
		semPreparados = new Semaphore(0,true);
		numPreparados=0;
		
		return "Reinicio realizado.";
	}
	
	@Path("finalizacion")
	@GET 
	@Produces(MediaType.TEXT_PLAIN)
	public String finalizacion() {
		//Un proceso avisa cuando ha finalizado
		semFinalizacion.release();
		
		return "";
	}
	
	@Path("fin")
	@GET 
	@Produces(MediaType.TEXT_PLAIN)
	public String fin() {
		//Espera a que los 6 procesos hayan finalizado
		try { semFinalizacion.acquire(6);} catch (InterruptedException e) {e.printStackTrace();}
		
		return "El algoritmo ISIS ha finalizado.";
	}
	
	@Path("iniciarProcesos")
	@GET 
	@Produces(MediaType.TEXT_PLAIN)
	public String iniciarProcesos(@QueryParam(value="dispatcherArbitro") String dispatcherArbitro,
			@QueryParam(value="id1") String id1,
			@QueryParam(value="ip1") String ip1,
			@QueryParam(value="id2") String id2,
			@QueryParam(value="ip2") String ip2,
			@QueryParam(value="id3") String id3,
			@QueryParam(value="ip3") String ip3,
			@QueryParam(value="id4") String id4,
			@QueryParam(value="ip4") String ip4,
			@QueryParam(value="id5") String id5,
			@QueryParam(value="ip5") String ip5,
			@QueryParam(value="id6") String id6,
			@QueryParam(value="ip6") String ip6,
			@QueryParam(value="rutaFichero") String rutaFichero,
			@QueryParam(value="ordenacionTotal") String ordenacionTotal) {
			
		Proceso p1 = new Proceso(dispatcherArbitro,id1,ip1,id2,ip2,id3,ip3,id4,ip4,id5,ip5,id6,ip6,rutaFichero,ordenacionTotal);
		Proceso p2 = new Proceso(dispatcherArbitro,id2,ip2,id1,ip1,id3,ip3,id4,ip4,id5,ip5,id6,ip6,rutaFichero,ordenacionTotal);
		
		procesos.put(id1, p1);
		procesos.put(id2, p2);
		procesos.get(id1).start();
		procesos.get(id2).start();
		
		return "Procesos iniciados.";
		
	}
	
	@Path("sincronizacion")
	@GET 
	@Produces(MediaType.TEXT_PLAIN)
	public String sincronizacion() {
		
		try { semAtomicoPreparados.acquire();} catch (InterruptedException e) {e.printStackTrace();}
		
		numPreparados++;
		
		if(numPreparados == 6) { 
			semAtomicoPreparados.release();
			//Como soy el último, aviso al resto
			semPreparados.release(5); 
		}
		else {
			semAtomicoPreparados.release();
			//Espero al resto de procesos
			try { semPreparados.acquire();} catch (InterruptedException e) {e.printStackTrace();}
			
		}
		
		return "Sincronizacion realizada.";
	}
	
	@Path("mensaje")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String mensaje(@QueryParam(value="cadenaMensaje") String cadenaMensaje,
			@QueryParam(value="procesoDestino") String procesoDestino) {
		
		// Redirige el mensaje al procesoDestino 
		String[] camposMensaje = cadenaMensaje.split(",");
		String procesoEmisorId = camposMensaje[3];
		procesos.get(procesoDestino).mensaje(cadenaMensaje, procesoEmisorId);
		
		return "Mensaje "+camposMensaje[1]+" redirigido al proceso "+procesoDestino;
		
	}
	
	@Path("propuesta")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String propuesta(@QueryParam(value="ordenPropuesto") String ordenPropuesto,
			@QueryParam(value="idMensaje") String idMensaje,
			@QueryParam(value="procesoDestino") String procesoDestino) {
		
		// Redirige la propuesta al procesoDestino
		procesos.get(procesoDestino).propuesta(ordenPropuesto, idMensaje);
		
		return "Propuesta redirigida al proceso "+procesoDestino+" idMensaje: "+idMensaje+" ordenPropuesto: "+ordenPropuesto;
		
	}
	
	@Path("acuerdo")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String acuerdo(@QueryParam(value="ordenAcordado") String ordenAcordado,
			@QueryParam(value="idMensaje") String idMensaje,
			@QueryParam(value="procesoDestino") String procesoDestino) {
		
		// Redirige el acuerdo al procesoDestino
		procesos.get(procesoDestino).acuerdo(ordenAcordado, idMensaje);
		
		return "Acuerdo redirigido al proceso "+procesoDestino+" idMensaje: "+idMensaje+" ordenAcordado: "+ordenAcordado;
		
	}

}
