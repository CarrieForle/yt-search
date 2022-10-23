import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.beans.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.lang.InterruptedException;
import org.json.*;
import java.util.Objects;

public class Main
{
	public static void main(String[] args)
	{
		var m = new GMainClass();
	}  
}

class Search
{
	String keyword;
	String token_next_page;
	String token_prev_page;
	int current_index = 0;
	int last_index;
	JSONArray li;
	boolean b_show_thumbnail = true;
	boolean b_show_channel = true;
	final String API_KEY = "AIzaSyCEmiiil3bIwwGXfIOEyOWzLYOfq4glNgs";
	final String ROOT = "https://www.googleapis.com/youtube/v3/search";
	
	public Search(String s)
	{
		keyword = Objects.requireNonNull(s);
	}
	
	public JSONArray page(String token, boolean is_go_to_next_page)
	{
		try
		{
			HttpResponse<String> http_response = request(token);
			
			if (http_response.statusCode() != 200)
			{
				System.out.format("Failed [%d]\n", http_response.statusCode());
				return new JSONArray();
			}
			
			var json_response = new JSONObject(http_response.body());
			
			li = json_response.getJSONArray("items");
			
			try
			{
				token_next_page = json_response.getString("nextPageToken");
				token_prev_page = json_response.getString("prevPageToken");
			}
			
			catch(Exception e)
			{
				if(is_go_to_next_page)
				{
					last_index = current_index;
				}
				
				else
				{
					token_prev_page = null;
				}
			}
			
			// System.out.print(json_response.toString(4));
		}
		catch(IOException | InterruptedException e)
		{
			System.out.format("An exception occurred: %s", e.toString());
		}
		
		if(is_go_to_next_page)
		{
			current_index++;
		}
		
		else // current_index < 0 checking is at GPage class
		{
			current_index--;
		}
		
		return li;
	}
	
	public JSONArray start()
	{
		try
		{
			HttpResponse<String> http_response = request();
			
			if (http_response.statusCode() != 200)
			{
				System.out.format("Failed [%d]\n", http_response.statusCode());
				return new JSONArray();
			}
			
			var json_response = new JSONObject(http_response.body());
			
			li = json_response.getJSONArray("items");
			try
			{
				token_next_page = json_response.getString("nextPageToken");
			}
			
			catch(Exception e)
			{
				last_index = current_index;
			}
			
			// System.out.print(json_response.toString(4));
		}
		
		catch(IOException | InterruptedException e)
		{
			System.out.format("An exception occurred: %s", e.toString());
		}
		
		return li;
	}
	
	private HttpResponse<String> request() throws IOException, InterruptedException
	{
		URI u = URI.create(ROOT + String.format("?part=snippet&key=%s&q=%s&maxResults=50&type=video,playlist", API_KEY, URLEncoder.encode(keyword, StandardCharsets.UTF_8)));
		
		HttpClient client = HttpClient.newHttpClient();
		
		HttpRequest request = HttpRequest.newBuilder()
		.GET()
		.uri(u)
		.build();
		
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}
	
	private HttpResponse<String> request(String token) throws IOException, InterruptedException
	{
		URI u = URI.create(ROOT + String.format("?part=snippet&key=%s&q=%s&maxResults=50&type=video,playlist&pageToken=%s", API_KEY, URLEncoder.encode(keyword, StandardCharsets.UTF_8), token));
		
		HttpClient client = HttpClient.newHttpClient();
		
		HttpRequest request = HttpRequest.newBuilder()
		.GET()
		.uri(u)
		.build();
		
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}
	
	public JSONArray result()
	{
		return li;
	}
	
	public String getNextPageToken()
	{
		return token_next_page;
	}
	
	public String getPrevPageToken()
	{
		return token_prev_page;
	}
	
	public int getCurrentIndex()
	{
		return current_index;
	}
}

class DownloadImage
{
	URL u;
	Image image;
	public DownloadImage(URL u)
	{
		this.u = u;
	}
	
	public void start()
	{
		Toolkit t = Toolkit.getDefaultToolkit();
		image = t.createImage(u);
	}
	
	public Image getImage()
	{
		return image;
	}
}

class GInfos implements ListSelectionListener
{
	private class ImagePanel extends JPanel implements MouseListener
	{
		Image image;
		String thumbnail_link;
		String youtube_link;
		
		ImagePanel()
		{
			super();
			addMouseListener(this);
		}
		
		void setYTLink(String l)
		{
			youtube_link = l;
		}
		
		void setIndex(int i)
		{
			if(thumbnail_links[i] == null) return;
			try
			{
				thumbnail_link = thumbnail_links[i];
				
				var d = new DownloadImage(new URL(thumbnail_link));
				d.start();
				
				image = d.getImage();
				// System.out.println(image == null);				
				repaint();
				
				setToolTipText("Open YouTube");
			}
			
			catch(MalformedURLException e)
			{
				System.out.println("Failed to fetch the thumbnail because the URL is broken");
			}
		}
		
		public void mouseClicked(MouseEvent ev)
		{
			try
			{
				Desktop.getDesktop().browse(URI.create(youtube_link));
			}
			catch(Exception e)
			{
				
			}
		}

		public void mouseEntered(MouseEvent e)
		{
			if(image == null) return;
			setCursor(Cursor.getDefaultCursor().getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		
		public void mouseExited(MouseEvent e)
		{
			setCursor(Cursor.getDefaultCursor().getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
		
		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
		}
	}
	
	private class CopyTextField implements ActionListener
	{
		JTextField tf;
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		
		CopyTextField(JTextField tf)
		{
			this.tf = tf;
		}
		
		public void actionPerformed(ActionEvent e)
		{
			String s = tf.getText();
			if(s.equals("")) return;
			
			var text = new StringSelection(s);
			//1st param: Transferable, 2nd param: ClipboardOwner (both extended by StringSelection)
			clipboard.setContents(text, text);
		}
	}
	
	private class BrowseTextField implements ActionListener
	{
		JTextField tf;
		
		BrowseTextField(JTextField tf)
		{
			this.tf = tf;
		}
		
		public void actionPerformed(ActionEvent ev)
		{
			try
			{
				Desktop.getDesktop().browse(URI.create(tf.getText()));
			}
			catch(Exception e)
			{
				
			}
		}
	}
	
	private int current_index_for_init;
	private boolean has_inited;
	JTextField link_field = new JTextField();
	JTextPane info = new JTextPane();
	ImagePanel p_thumbnail = new ImagePanel();
	JButton bt_copy = new JButton("Copy");
	JButton bt_browse = new JButton("Browse");
	String[] channels;
	String[] titles;
	String[] ids;
	String[] descriptions;
	String[] thumbnail_links;
	boolean[] is_videos;
	
	public GInfos()
	{
		info.setEditable(false);
		link_field.setEditable(false);
		link_field.setBackground(Color.WHITE);
		link_field.setFont(new Font("Arial", Font.BOLD, 16));
		
		bt_copy.addActionListener(new CopyTextField(link_field));
		bt_browse.addActionListener(new BrowseTextField(link_field));
		
		Dimension d = new Dimension(480, 360);
		p_thumbnail.setPreferredSize(d);
		p_thumbnail.setSize(d);
		p_thumbnail.setMaximumSize(d);
	}
	
	public void init(int length)
	{
		has_inited = true;
		current_index_for_init = 0;
		channels = new String[length];
		titles = new String[length];
		ids = new String[length];
		is_videos = new boolean[length];
		thumbnail_links = new String[length];
		descriptions = new String[length];
	}
	
	public void add(String c, String t, String id, boolean is_video, String l, String d)
	{
		channels[current_index_for_init] = c;
		titles[current_index_for_init] = t;
		ids[current_index_for_init] = id;
		is_videos[current_index_for_init] = is_video;
		thumbnail_links[current_index_for_init] = l;
		descriptions[current_index_for_init] = d;
		
		current_index_for_init++;
	}
	
	public void valueChanged(ListSelectionEvent ev)
	{
		int i = ((ListSelectionModel)ev.getSource()).getMinSelectionIndex();
		
		// init() has called at least once || chosen to not chosen (ex t.clearSelection())
		if(!has_inited || i == -1) return;
		
		String link;
		
		if(titles[i] == null)
		{
			return;
		}
		
		if(is_videos[i])
		{
			link = "https://youtu.be/" + ids[i];
		}
		
		else
		{
			link = "https://youtube.com/playlist?list=" + ids[i];
		}
		
		System.out.println("valueChanged");
		p_thumbnail.setIndex(i);
		p_thumbnail.setYTLink(link);
		// AttributeSet att = new SimpleAttributeSet();
		link_field.setText(link);
		
		info.setText(
			String.format("Type: %s\nChannel: %s\nTitle: %s\nDescription: %s",
				(is_videos[i] ? "Video" : "Playlist"),
				channels[i],
				titles[i],
				descriptions[i] == "" ? "None" : descriptions[i]
			)
		);
	}
	
	public JTextPane getTextPane()
	{
		return info;
	}
	
	public JTextField getLinkField()
	{
		return link_field;
	}
	
	public JPanel getThumbnail()
	{
		return p_thumbnail;
	}
	
	public JButton getCopyButton()
	{
		return bt_copy;
	}
	public JButton getBrowseButton()
	{
		return bt_browse;
	}
}

class GPage implements ActionListener
{
	JButton bt_prev;
	JButton bt_next;
	GSearch gs;
	
	public GPage(JButton bt_prev, JButton bt_next, GSearch gs)
	{
		this.bt_prev = bt_prev;
		this.bt_next = bt_next;
		this.gs = gs;
		
		bt_prev.addActionListener(this);
		bt_next.addActionListener(this);
	}
	
	public void actionPerformed(ActionEvent ev)
	{
		if(gs.getSearch() == null) return;
		
		Search s = gs.getSearch();
		
		if(bt_prev == ev.getSource())
		{
			System.out.print("prev ");
			// if(s.getCurrentIndex() == 0 || s.prevPageToken() == null) return;
			
			// else
			// {
				gs.tablePage(false);
				// gs.page(s.prevPageToken(), false);
			// }
		}
		
		else
		{
			System.out.print("next ");
			// gs.page(s.nextPageToken(), true);
			gs.tablePage(true);
		}
		// System.out.format("Current index = %d\n", s.getCurrentIndex());
	}
	
	public JButton getPrevButton()
	{
		return bt_prev;
	}
	
	public JButton getNextButton()
	{
		return bt_next;
	}
}

class GSearch implements ActionListener
{
	private int items_per_page = 10;
	private int current_index = 0;
	private boolean is_at_the_final_page = false;
	JTextField tf;
	JButton b;
	JTable t;
	TableModel tm;
	Search search;
	String last_search_keyword;
	JSONArray jsona_items;
	GInfos infos = new GInfos();
	
	public GSearch(JTextField tf, JButton b, JTable t)
	{
		this.tf = tf;
		this.b = b;
		this.t = t;
		
		ListSelectionModel lsm = t.getSelectionModel();
		lsm.addListSelectionListener(infos);
		
		tm = new DefaultTableModel(new String[]{"Channel", "Title"}, 10)
		{
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		
		t.setModel(tm);
		t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		t.getColumnModel().getColumn(1).setPreferredWidth(300);
		t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		t.getTableHeader().setReorderingAllowed(false);
		t.setDragEnabled(true);
		
		b.addActionListener(this);
	}
	
	// JSONArrays must be constructed here, not in Search class 
	public void actionPerformed(ActionEvent e)
	{
		String keyword = tf.getText().strip();
		if(keyword.equals("") || keyword.equals(last_search_keyword)) return;
		
		infos.init(items_per_page);
		
		System.out.println("actionPerformed");
		
		search = new Search(keyword);
		
		jsona_items = search.start();
		
		makeTable(0, items_per_page);
		
		current_index = 0;
		last_search_keyword = keyword;
	}
	
	public void tablePage(boolean is_go_to_next_page)
	{
		if(!is_go_to_next_page && current_index == 0) return;
		if(is_go_to_next_page && is_at_the_final_page) return;
		
		is_at_the_final_page = false;
		
		String keyword = tf.getText().strip();
		if(!keyword.equals(last_search_keyword)) return;
		
		if(is_go_to_next_page && items_per_page * (current_index+1) >= 50 * (search.getCurrentIndex()+1))
		{
			current_index++;
			page(search.getNextPageToken(), true);
			return;
		}
		
		else if(!is_go_to_next_page && items_per_page * (current_index-1) < 50 * search.getCurrentIndex())
		{
			current_index--;
			page(search.getPrevPageToken(), false);
			return;
		}
		
		infos.init(items_per_page);
		
		System.out.println("tablePagePerformed");
		
		if(is_go_to_next_page)
		{
			current_index++;
		}
		
		else
		{
			current_index--;
		}
		
		System.out.println(current_index);
		
		int end = (items_per_page * (current_index+1)) % 50;
		if(end == 0) end = 50;
		
		makeTable((items_per_page * current_index) % 50, end);
	}
	
	private void page(String token, boolean is_go_to_next_page)
	{
		infos.init(items_per_page);
		
		System.out.println("pagePerformed");
		
		jsona_items = search.page(token, is_go_to_next_page);
		
		System.out.println(current_index);
		
		makeTable(0, items_per_page);
	}
	
	private void makeTable(int start_index, int end_index)
	{
		int i = start_index;
		try
		{
			for(; i < end_index; i++)
			{
				boolean is_video;
				
				String channel = jsona_items.getJSONObject(i)
				.getJSONObject("snippet")
				.getString("channelTitle");
				
				String title = jsona_items.getJSONObject(i)
				.getJSONObject("snippet")
				.getString("title");
				
				String description = jsona_items.getJSONObject(i)
				.getJSONObject("snippet")
				.getString("description");
				
				String id;
				if(
					jsona_items.getJSONObject(i)
					.getJSONObject("id")
					.getString("kind").equals("youtube#video")
				)
				{
					id = jsona_items.getJSONObject(i)
					.getJSONObject("id")
					.getString("videoId");
					
					is_video = true;
				}
				
				else
				{
					id = jsona_items.getJSONObject(i)
					.getJSONObject("id")
					.getString("playlistId");
					
					is_video = false;
				}
				
				String thumbnail_link = jsona_items.getJSONObject(i)
				.getJSONObject("snippet")
				.getJSONObject("thumbnails")
				.getJSONObject("high")
				.getString("url");
				
				tm.setValueAt(channel, i % items_per_page, 0);
				tm.setValueAt(title, i % items_per_page, 1);
				
				infos.add(channel, title, id, is_video, thumbnail_link, description);
			}
		}
		
		catch(JSONException e)
		{
			for(; i < end_index; i++)
			{
				tm.setValueAt("", i % items_per_page, 0);
				tm.setValueAt("", i % items_per_page, 1);
			}

			is_at_the_final_page = true;
		}
		
		t.clearSelection();
	}
	
	public JTable getTable()
	{
		return t;
	}
	
	public GInfos getGInfos()
	{
		return infos;
	}
	
	public Search getSearch()
	{
		return search;
	}
}

class GMainClass
{
	JFrame f;
	JTextField tf_input;
	JButton bt_search;
	JButton bt_prev;
	JButton bt_next;
	JPanel p_input;
	JPanel p_info;
	JTable t_table;
	
	final int margin = 10;
	final int minWidth = 10;
	final int minHeight = 10;
	
	public GMainClass()
	{
		f = new JFrame("YouTube Search");
		
		tf_input = new JTextField();
		
		bt_search = new JButton("Search");
		bt_search.setSize(100, 40);
		
		bt_prev = new JButton("Prev");
		bt_prev.setSize(100, 40);
		
		bt_next = new JButton("Next");
		bt_next.setSize(100, 40);
		
		t_table = new JTable();
		
		var s_search = new GSearch(tf_input, bt_search, t_table);
		GInfos GI = s_search.getGInfos();
		var GP = new GPage(bt_prev, bt_next, s_search);
		
		var sp_table = new JScrollPane(t_table);
		sp_table.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		var p_info = new Box(BoxLayout.Y_AXIS)
		{
			@Override
			public void doLayout()
			{
				JPanel p_thumbnail = (JPanel)getComponent(0);
				
				int thumbnail_width = getWidth();
				int thumbnail_height = (int)(thumbnail_width * 0.75);
				
				p_thumbnail.setPreferredSize(new Dimension(thumbnail_width, thumbnail_height));
				p_thumbnail.setSize(thumbnail_width, thumbnail_height);
				
				
				JScrollPane sp = (JScrollPane)getComponent(1);
				JViewport v = sp.getViewport();
				JTextPane tp = (JTextPane)v.getView();
				
				tp.setPreferredSize(new Dimension(thumbnail_width, getHeight() - thumbnail_height));
				tp.setSize(thumbnail_width, getHeight() - thumbnail_height);
				
				super.doLayout();
			}
		};
		
		p_info.setMinimumSize(new Dimension(120, 1));
		
		p_info.add(GI.getThumbnail());
		p_info.add(new JScrollPane(GI.getTextPane()));

		var p_main = new JPanel(new GridBagLayout())
		{
			@Override
			public void doLayout()
			{
				JScrollPane sp = (JScrollPane)getComponent(2);
				JViewport v = sp.getViewport();
				JTable t = (JTable)v.getView();
				
				t.setPreferredScrollableViewportSize(new Dimension(getWidth() - margin * 5, 160));
				// t.setSize(t.getPreferredScrollableViewportSize());
				setMinimumSize(new Dimension(getParent().getWidth() - 480, 160));
				
				// JTextField tf = (JTextField)getComponent(3);
				// tf.setSize(new Dimension(getWidth(), tf.getHeight()));
				
				super.doLayout();
			}
		};
		
		var gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.gridwidth = 1;
		p_main.add(bt_search, gbc);
		
		gbc.weightx = 1;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		p_main.add(tf_input, gbc);
		p_main.add(sp_table, gbc);
		
		gbc.gridwidth = 3;
		p_main.add(GI.getLinkField(), gbc);
		
		gbc.weightx = 0;
		gbc.gridwidth = 1;
		p_main.add(GI.getCopyButton(), gbc);
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		p_main.add(GI.getBrowseButton(), gbc);

		gbc.gridwidth = 1;
		p_main.add(bt_prev, gbc);
		p_main.add(bt_next, gbc);
		
		var sp_main = new JSplitPane(
			JSplitPane.HORIZONTAL_SPLIT,
			true,
			p_main,
			p_info
		);
		
		sp_main.setSize(1200, 600);
		sp_main.setDividerLocation(0.7);
		sp_main.setDividerSize(10);
		sp_main.setResizeWeight(0.5);
		sp_main.resetToPreferredSizes();
		// sp_main.validate();
		
		f.add(sp_main);
		
		f.setSize(1200, 600); //width and height
		// f.setLayout(new GridLayout(1, 2)); 
		f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // Dispose the frame instead of hiding when attempting to close
		f.setVisible(true); // Making the frame visible 
	}
}