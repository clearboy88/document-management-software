package com.logicaldoc.gui.frontend.client.folder;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.logicaldoc.gui.common.client.Session;
import com.logicaldoc.gui.common.client.beans.GUIFolder;
import com.logicaldoc.gui.common.client.log.Log;
import com.logicaldoc.gui.common.client.observer.FolderController;
import com.logicaldoc.gui.common.client.observer.FolderObserver;
import com.logicaldoc.gui.common.client.util.ItemFactory;
import com.logicaldoc.gui.frontend.client.services.FolderService;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.SpinnerItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

/**
 * A cursor to browse among pages in a folders tree. If pagination is enabled.
 * 
 * @author Marco Meschieri - LogicalDOC
 * @since 8.2.1
 */
public class FolderCursor extends DynamicForm implements FolderObserver {

	private static FolderCursor instance = null;

	private SpinnerItem maxItem;

	private SpinnerItem pageItem;

	private FolderPagination currentPagination = new FolderPagination(0L, 1000, 0, 1);

	private Map<Long, FolderPagination> paginations = new HashMap<Long, FolderPagination>();

	public static FolderCursor get() {
		if (instance == null)
			instance = new FolderCursor();
		return instance;
	}

	public FolderCursor() {
		setNumCols(3);
		setHeight(1);

		maxItem = ItemFactory.newSpinnerItem("max", "display", Session.get().getConfigAsInt("gui.folder.maxchildren"),
				2, (Integer) null);
		maxItem.setWidth(60);
		maxItem.setStep(20);
		maxItem.setSaveOnEnter(true);
		maxItem.setImplicitSave(true);
		maxItem.setShowTitle(false);
		maxItem.addChangedHandler(new ChangedHandler() {

			@Override
			public void onChanged(ChangedEvent event) {
				onMaxChange();
			}
		});

		pageItem = ItemFactory.newSpinnerItem("page", "page", 1, 1, 1);
		pageItem.setHint("");
		pageItem.setSaveOnEnter(true);
		pageItem.setImplicitSave(true);
		pageItem.setShowTitle(false);
		pageItem.setWidth(45);
		pageItem.addChangedHandler(new ChangedHandler() {

			@Override
			public void onChanged(ChangedEvent event) {
				onPageChange();
			}
		});

		SpacerItem spacer = new SpacerItem();
		spacer.setWidth(6);
		spacer.setTitle("|");

		setItems(maxItem, spacer, pageItem);

		FolderController.get().addObserver(this);
	}

	public void setTotalRecords(int totalRecords) {
		currentPagination.setTotalElements(totalRecords);
		update();
	}

	private void onMaxChange() {
		if (maxItem.validate()) {
			currentPagination.setPage(1);
			currentPagination.setPageSize(Integer.parseInt(maxItem.getValue().toString()));
			update();
		}
	}

	private void onPageChange() {
		if (pageItem.validate()) {
			currentPagination.setPage(Integer.parseInt(pageItem.getValue().toString()));
			update();
		}
	}

	public void registerMaxChangedHandler(ChangedHandler handler) {
		maxItem.addChangedHandler(handler);
	}

	public void registerPageChangedHandler(ChangedHandler handler) {
		pageItem.addChangedHandler(handler);
	}

	@Override
	public void onFolderSelected(GUIFolder folder) {
		FolderPagination pagination = paginations.get(folder.getId());
		if (pagination == null) {
			pagination = new FolderPagination(folder.getId(), Session.get().getConfigAsInt("gui.folder.maxchildren"),
					folder.getSubfolderCount(), 1);
			// Save it only if there are more than one page
			if (pagination.getTotalPages() >= 2)
				paginations.put(folder.getId(), pagination);
			currentPagination = pagination;
		} else {
			pagination.setTotalElements(folder.getSubfolderCount());
			currentPagination = pagination;

			// Remove from client and server if there are less than two pages
			if (pagination.getTotalPages() < 2)
				paginations.remove(folder.getId());
		}
		update();
	}

	private void update() {
		updateClient();
		updateServer();
	}

	private void updateClient() {
		maxItem.setValue(currentPagination.getPageSize());
		pageItem.setValue(currentPagination.getPage());
		pageItem.setMax(currentPagination.getTotalPages());
	}

	public boolean hasMorePages() {
		return currentPagination.getPage() < currentPagination.getTotalPages();
	}

	/**
	 * Moves to the next page and updates the client(not the server)
	 */
	public void next() {
		if (hasMorePages()) {
			currentPagination.setPage(currentPagination.getPage() + 1);
		} else
			currentPagination.setPage(1);
		updateClient();
	}

	public long getFolderId() {
		return currentPagination.getFolderId();
	}

	private void updateServer() {
		if (currentPagination.getTotalPages() < 2
				&& currentPagination.getPageSize() == Session.get().getConfigAsInt("gui.folder.maxchildren"))
			FolderService.Instance.get().setFolderPagination(currentPagination.getFolderId(), null, null,
					new AsyncCallback<Void>() {

						@Override
						public void onFailure(Throwable caught) {
							Log.serverError(caught);
						}

						@Override
						public void onSuccess(Void arg) {

						}
					});
		else
			FolderService.Instance.get().setFolderPagination(currentPagination.getFolderId(),
					currentPagination.getStartRow(), currentPagination.getPageSize(), new AsyncCallback<Void>() {

						@Override
						public void onFailure(Throwable caught) {
							Log.serverError(caught);
						}

						@Override
						public void onSuccess(Void arg) {

						}
					});
	}

	@Override
	public void onFolderChanged(GUIFolder folder) {

	}

	@Override
	public void onFolderDeleted(GUIFolder folder) {

	}

	@Override
	public void onFolderCreated(GUIFolder folder) {

	}

	@Override
	public void onFolderMoved(GUIFolder folder) {

	}

	public FolderPagination getCurrentPagination() {
		return currentPagination;
	}
}