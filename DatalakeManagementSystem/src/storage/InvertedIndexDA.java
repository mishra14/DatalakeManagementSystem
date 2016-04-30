package storage;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import bean.InvertedIndex;
import bean.Occurance;

public class InvertedIndexDA {

	private static MongoClientURI URI = new MongoClientURI(
			"mongodb://dlms_webapp:webapp@ds013971.mlab.com:13971/webappdb");
	public static String COLLECTION_NAME = "ii_test";
	public static String WORD_KEY = "word";
	public static String OCCURS_KEY = "occurs";
	private MongoClient client;
	private MongoDatabase db;
	private MongoCollection<Document> collection;

	public InvertedIndexDA() {
		super();
		this.client = new MongoClient(URI);
		this.db = client.getDatabase(URI.getDatabase());
		db.getCollection(COLLECTION_NAME).createIndex(new Document(WORD_KEY, 1), new IndexOptions().unique(true));
		this.collection = db.getCollection(COLLECTION_NAME);
	}

	public MongoClient getClient() {
		return client;
	}

	public MongoDatabase getDb() {
		return db;
	}

	@SuppressWarnings("unchecked")
	public InvertedIndex fetch(String username) {
		Document doc = collection.find(eq(WORD_KEY, username)).first();
		System.out.println("Fetched doc : " + doc);
		InvertedIndex iIndex = null;
		if (doc != null) {
			List<Occurance> occurs = new ArrayList<Occurance>();
			for (Document o : (ArrayList<Document>) doc.get(OCCURS_KEY)) {
				occurs.add(new Occurance(o.getString("path"), o.getString("attribute")));
			}
			iIndex = new InvertedIndex(doc.getString(WORD_KEY), occurs);
		}
		return iIndex;
	}

	public void store(InvertedIndex iIndex) {
		Document doc = Document.parse(new JSONObject(iIndex).toString());
		collection.insertOne(doc);
	}

	public void update(InvertedIndex iIndex) {
		/*Document doc = Document.parse(new JSONObject(iIndex).toString());
		System.out.println(new JSONArray(iIndex.getOccurs()));
		collection.updateOne(eq(WORD_KEY, iIndex.getWord()),
				set(OCCURS_KEY, doc.get(OCCURS_KEY)));*/
		delete(iIndex);
		store(iIndex);
	}

	public void delete(InvertedIndex iIndex) {
		collection.deleteOne(eq(WORD_KEY, iIndex.getWord()));
	}

	public void delete(String username) {
		collection.deleteOne(eq(WORD_KEY, username));
	}

	public void close() {
		client.close();
	}

	public static void main(String[] args) {
		ArrayList<Occurance> occurs = new ArrayList<Occurance>();
		occurs.add(new Occurance("user1_tst.xml/title", "title"));
		occurs.add(new Occurance("user1_tst2.xml/title", "title"));
		occurs.add(new Occurance("user1_tst.xml/content", "content"));
		InvertedIndex iIndex = new InvertedIndex("test_word", occurs);
		InvertedIndexDA iIndexDA = new InvertedIndexDA();
		try {
			
			iIndexDA.store(iIndex);
			System.out.println(iIndexDA.fetch("test_word"));
			occurs.add(new Occurance("user1_tst2.xml/owner", "owner"));
			iIndexDA.update(iIndex);
			System.out.println(iIndexDA.fetch("test_word"));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			iIndexDA.delete("test_word");
			System.out.println(iIndexDA.fetch("test_word"));
			iIndexDA.close();
		}
	}

}
