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
	private Map<Integer, Airport> idMap;
	private Map<Airport, Airport> visita;
	
	public Model() {
		dao = new ExtFlightDelaysDAO();
		idMap = new HashMap<Integer, Airport>();
		dao.loadAllAirports(idMap);
	}
	
	public void creaGrafo(int x) {
		grafo = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		//aggiungo vertici 'filtrati'
		Graphs.addAllVertices(grafo, dao.getVertici(idMap, x));
		
		//aggiungo gli archi
		for(Rotta r : dao.getRotte(idMap)) {
			if(this.grafo.containsVertex(r.getA1()) && this.grafo.containsVertex(r.getA2())) {
				DefaultWeightedEdge e = this.grafo.getEdge(r.getA1(), r.getA2());
				if(e == null) {
					Graphs.addEdgeWithVertices(grafo, r.getA1(), r.getA2(), r.getN());
				}
				else {
					double pesoVecchio = this.grafo.getEdgeWeight(e);
					double pesoNuovo = pesoVecchio + r.getN();
					this.grafo.setEdgeWeight(e, pesoNuovo);
				}
			}
		}
		System.out.println("Grafo creato");
		System.out.println("#Vertici: " + grafo.vertexSet().size());
		System.out.println("#Archi: " + grafo.edgeSet().size());
	}

	public Set<Airport> getVertici() {
		return this.grafo.vertexSet();
	}
	
	public List<Airport> trovaPercorso(Airport a1, Airport a2){
		List<Airport> percorso = new LinkedList<>();
		
		BreadthFirstIterator<Airport, DefaultWeightedEdge> it = new BreadthFirstIterator<Airport, DefaultWeightedEdge>(grafo, a1);
		
		visita = new HashMap<>();
		visita.put(a1, null);
		
		it.addTraversalListener(new TraversalListener<Airport, DefaultWeightedEdge>() {
			
			@Override
			public void vertexTraversed(VertexTraversalEvent<Airport> e) {}		
			@Override
			public void vertexFinished(VertexTraversalEvent<Airport> e) {}		
			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultWeightedEdge> e) {
				Airport sorgente = grafo.getEdgeSource(e.getEdge());
				Airport destinazione = grafo.getEdgeTarget(e.getEdge());
				
				if(visita.containsKey(sorgente) && !visita.containsKey(destinazione)) {
					visita.put(sorgente, destinazione);
				}
				else if(visita.containsKey(destinazione) && !visita.containsKey(sorgente)) {
					visita.put(destinazione, sorgente);
				}
			}
			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {}		
			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {}
		});
		
		while(it.hasNext()) {
			it.next();
		}
		
		if(!visita.containsKey(a1) || !visita.containsKey(a2))
			return null;
		
		percorso.add(a2);
		Airport step = a2;
		while(visita.get(step) != null) {
			step = visita.get(step);
			percorso.add(0, step);
		}
		
		return percorso;
	}
}
