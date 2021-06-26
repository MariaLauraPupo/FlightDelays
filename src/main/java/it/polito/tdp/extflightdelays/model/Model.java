package it.polito.tdp.extflightdelays.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graphs;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import it.polito.tdp.extflightdelays.db.ExtFlightDelaysDAO;

public class Model {
	private SimpleWeightedGraph<Airport, DefaultWeightedEdge> grafo;
	private ExtFlightDelaysDAO dao;
	private Map<Integer,Airport> idMap;
	private Map<Airport,Airport> visita;//mappa dove salvo l'albero di visita
	
	public Model() {
		dao = new ExtFlightDelaysDAO();
		idMap = new HashMap<Integer,Airport>();
		//riempio l'idMap
		dao.loadAllAirports(idMap);
	}
	
	public void creaGrafo(int x) { //x numero di compagnie aeree minime con il quale v
		                           //dobbiamo filtrare i nostri vertici
		grafo = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		//non si può fare "Graphs.addAllVertices(grafo,idMap)"
		//perchè questo è quello che facciamo quando nel grafo vogliamo avere tutti i vertici
		//in questo caso invece vogliamo solo un sottoinsieme di vertici
		Graphs.addAllVertices(grafo, dao.getVertici(x, idMap));
		
		//aggiungo gli archi
		//l'informazione che c'è dal nodo A al nodo B può essere diversa 
		//da quella che c'è  dal nodo B al nodo A 
		for(Rotta r:dao.getRotte(idMap)) {
			//controllo che la rotta sia presente nel grafo
			if(this.grafo.containsVertex(r.getA1()) && this.grafo.containsVertex(r.getA2())) {
				//vado a vedere se c'è già un arco tra i due nodi, 
				//se non c'è vuol dire che è la prima volta che vedo la rotta
				//se c'è già vuol dire che sto considerando la rotta inversa
				DefaultWeightedEdge e = this.grafo.getEdge(r.getA1(), r.getA2());//-> nuovo arco tra A1 e A2
				//è un arco NON ORIENTATO, mi dice semplicemente se tra quei due vertici c'è o no l'arco
				//non è importante l'ordine dei paramentri 
				if(e == null) {
					//non c'è ancora un arco tra questi due vertici
					Graphs.addEdgeWithVertices(grafo, r.getA1(), r.getA2());
					//salvo l'arco con il primo peso che ho trovato
				}else {
					//ho già incotrato questo arco
					//ne incremento solamente il peso
					double pesoVecchio = this.grafo.getEdgeWeight(e);
					double pesoNuovo = pesoVecchio + r.getN();
					this.grafo.setEdgeWeight(e, pesoNuovo);
				}
				
			}
		}
		System.out.println("Grafo creato");
		System.out.println("numero Vertici: " + grafo.vertexSet().size());
		System.out.println("numero Archi: " + grafo.edgeSet().size());
		
	}

	public Set<Airport> getVertici() {
		return this.grafo.vertexSet();
	}
    //punto d. devo fare una vista del grafo
	
	//Metodo che ritorni una lista di nodi che rappresentano il percorso
	//se la lista è vuota vuol dire che il percorso tra quei due nodi non esiste	
	public List<Airport> trovaPercorso (Airport a1, Airport a2){
		List<Airport> percorso = new LinkedList<>();
		
		//visita in ampiezza
		//creo l'iteratore
		BreadthFirstIterator<Airport, DefaultWeightedEdge> it = new BreadthFirstIterator<>(grafo,a1);
		
		//mappa per salvare l'albero di visita
	    visita = new HashMap<>();
		visita.put(a1, null);//radice
		it.addTraversalListener(new TraversalListener<Airport, DefaultWeightedEdge>(){//metti le parentesi graffe per poter aggiungere i metodi
//      alternativa usare getParent()
			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
				
			}

			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
				
			}

			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultWeightedEdge> e) {
				//a noi interessa questo metodo
				Airport airport1 = grafo.getEdgeSource(e.getEdge());
				Airport airport2 = grafo.getEdgeTarget(e.getEdge());
				
				//vado a vedere nella mappa
				//stiamo parlando di grafi NON ORIENTATI => dobbiamo capire quale dei due nodi abbiamo già visitato
                if (visita.containsKey(airport1) && !visita.containsKey(airport2)) {
                	visita.put(airport2, airport1);
                }else if(visita.containsKey(airport2) && !visita.containsKey(airport1)) {
                	visita.put(airport1, airport2);
                }
			}

			@Override
			public void vertexTraversed(VertexTraversalEvent<Airport> e) {
				
			}

			@Override
			public void vertexFinished(VertexTraversalEvent<Airport> e) {
				
			}
			
		});
		while(it.hasNext()) {
			it.next();
		}
		//ricordati di mettere la radice
		percorso .add(a2);//vado all'indietro, quindi aggiungo la destinazione
		Airport step = a2;
		while(visita.get(step) != null) {
			step = visita.get(step);//vado a vedere chi era suo padre
			percorso.add(step);
		}
		
		return percorso;
	}
}
