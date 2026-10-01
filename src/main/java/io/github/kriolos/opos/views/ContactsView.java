package io.github.kriolos.opos.views;

import java.util.ArrayList;
import java.util.List;

import io.github.kriolos.opos.model.Contact;
import io.github.kriolos.opos.repository.ContactRepository;

import com.webforj.Page;
import com.webforj.component.Composite;
import com.webforj.component.Expanse;
import com.webforj.component.Theme;
import com.webforj.component.badge.BadgeTheme;
import com.webforj.component.button.Button;
import com.webforj.component.button.ButtonTheme;
import com.webforj.component.dialog.Dialog;
import com.webforj.component.field.TextField;
import com.webforj.component.html.elements.H2;
import com.webforj.component.html.elements.Paragraph;
import com.webforj.component.icons.TablerIcon;
import com.webforj.component.layout.flexlayout.FlexDirection;
import com.webforj.component.layout.flexlayout.FlexJustifyContent;
import com.webforj.component.layout.flexlayout.FlexLayout;
import com.webforj.component.list.ChoiceBox;
import com.webforj.component.navigator.Navigator;
import com.webforj.component.table.Column;
import com.webforj.component.table.Table;
import com.webforj.component.table.renderer.AvatarRenderer;
import com.webforj.component.table.renderer.BadgeRenderer;
import com.webforj.component.table.renderer.ConditionalRenderer;
import com.webforj.component.toast.Toast;
import com.webforj.data.Paginator;
import com.webforj.router.annotation.FrameTitle;
import com.webforj.router.annotation.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

/**
 * ContactsView — full-featured contacts list with internal scrolling and optional pagination.
 *
 * <h3>Responsive & Navigation strategy:</h3>
 * <ul>
 * <li>Table fills available vertical space with internal scrolling for large datasets.</li>
 * <li>Optional pagination with {@link Navigator} (pages, next, prev, first, last) and page-size selector (5, 10, 20, All).</li>
 * <li>Avatar + Name columns are pinned LEFT for responsive horizontal scroll.</li>
 * <li>Secondary columns are selectively hidden on small screens via media queries.</li>
 * </ul>
 */
@Route(value = "/contacts", outlet = MainLayout.class)
@FrameTitle("Contacts")
public class ContactsView extends Composite<FlexLayout> {

    private final FlexLayout self = getBoundComponent();
    private Table<Contact> table;
    private TextField searchField;

    @Inject
    private ContactRepository repository;

    // Stat card counter labels
    private Paragraph customersCountLabel;
    private Paragraph suppliersCountLabel;
    private Paragraph employeesCountLabel;

    // Responsive columns
    private Column<Contact, String> companyCol;
    private Column<Contact, String> emailCol;
    private Column<Contact, String> phoneCol;

    // Data, Pagination & Scroll
    private List<Contact> allContacts = new ArrayList<>();
    private Navigator navigator;
    private ChoiceBox pageSizeSelect;
    private Paragraph pageInfoText;

    public ContactsView() {
    }

    @PostConstruct
    public void init() {
        buildUi();
        injectResponsiveCss();
    }

    // ---------------------------------------------------------------------------
    // UI
    // ---------------------------------------------------------------------------
    private void buildUi() {
        self.setDirection(FlexDirection.COLUMN);
        self.setStyle("padding", "1.5rem");
        self.setStyle("gap", "0.85rem");
        self.setHeight("100%");
        self.setStyle("box-sizing", "border-box");
        self.setStyle("min-height", "0");
        self.setStyle("overflow", "hidden");

        self.add(buildHeader(), buildStats(), buildTable(), buildPaginationBar());
        refreshTable("");
    }

    // ── Header ──────────────────────────────────────────────────────────────────
    private FlexLayout buildHeader() {
        H2 title = new H2("Contacts");
        title.setStyle("margin", "0");
        title.setStyle("font-size", "1.5rem");
        title.setStyle("font-weight", "600");

        searchField = new TextField();
        searchField.setPlaceholder("Search contacts…");
        searchField.setPrefixComponent(TablerIcon.create("search"));
        searchField.setStyle("min-width", "200px");
        searchField.setStyle("flex", "1");
        searchField.setStyle("max-width", "340px");
        searchField.onValueChange(ev -> refreshTable(ev.getValue()));

        Button addBtn = new Button("Add contact")
                .setPrefixComponent(TablerIcon.create("plus"))
                .setTheme(ButtonTheme.PRIMARY);
        addBtn.onClick(ev -> openAddDialog());

        FlexLayout right = FlexLayout.create(searchField, addBtn)
                .align().center()
                .build();
        right.setStyle("gap", ".5rem");
        right.setStyle("flex", "1");
        right.setStyle("flex-wrap", "wrap");
        right.setStyle("justify-content", "flex-end");

        FlexLayout header = FlexLayout.create(title, right)
                .align().center()
                .build();
        header.setStyle("gap", "1rem");
        header.setStyle("flex-wrap", "wrap");
        header.setStyle("flex-shrink", "0");
        return header;
    }

    // ── Stat cards ──────────────────────────────────────────────────────────────
    private FlexLayout buildStats() {
        long customers = (repository != null) ? repository.countByType("CUSTOMER") : 0;
        long suppliers = (repository != null) ? repository.countByType("SUPPLIER") : 0;
        long employees = (repository != null) ? repository.countByType("EMPLOYEE") : 0;

        customersCountLabel = new Paragraph(String.valueOf(customers));
        suppliersCountLabel = new Paragraph(String.valueOf(suppliers));
        employeesCountLabel = new Paragraph(String.valueOf(employees));

        FlexLayout stats = FlexLayout.create(
                buildStatCard("Customers", customersCountLabel, "users", "var(--dwc-color-primary)"),
                buildStatCard("Suppliers", suppliersCountLabel, "building-store", "var(--dwc-color-success)"),
                buildStatCard("Employees", employeesCountLabel, "id-badge", "var(--dwc-color-warning)")
        ).build();
        stats.setStyle("gap", "1rem");
        stats.setStyle("flex-wrap", "wrap");
        stats.setStyle("flex-shrink", "0");
        return stats;
    }

    private FlexLayout buildStatCard(String label, Paragraph countLabel, String icon, String color) {
        var ico = TablerIcon.create(icon);
        ico.setStyle("font-size", "1.4rem");
        ico.setStyle("color", color);

        countLabel.setStyle("font-size", "1.6rem");
        countLabel.setStyle("font-weight", "700");
        countLabel.setStyle("margin", "0");
        countLabel.setStyle("color", color);

        Paragraph lbl = new Paragraph(label);
        lbl.setStyle("font-size", ".8rem");
        lbl.setStyle("color", "var(--dwc-color-gray-text-light)");
        lbl.setStyle("margin", "0");

        FlexLayout text = FlexLayout.create(countLabel, lbl).vertical().build();
        FlexLayout card = FlexLayout.create(ico, text).align().center().build();
        card.setStyle("gap", ".75rem");
        card.setStyle("padding", ".75rem 1.25rem");
        card.setStyle("background", "var(--dwc-color-default-alt)");
        card.setStyle("border-radius", "10px");
        card.setStyle("border", "1px solid var(--dwc-color-default-border)");
        card.setStyle("min-width", "130px");
        card.setStyle("flex", "1 1 130px");
        return card;
    }

    // ── Table ───────────────────────────────────────────────────────────────────
    private Table<Contact> buildTable() {
        table = new Table<>();
        table.setStyle("flex", "1 1 0");
        table.setStyle("width", "100%");
        table.setStyle("min-height", "0");
        table.setHeight("100%");

        // ── Avatar (pinned left) ──
        var avatarCol = table.addColumn("avatar", Contact::name);
        avatarCol.setLabel("");
        avatarCol.setWidth(58);
        avatarCol.setSortable(false);
        avatarCol.setResizable(false);
        avatarCol.setPinDirection(Column.PinDirection.LEFT);
        avatarCol.setRenderer(new AvatarRenderer<>());

        // ── Name (pinned left, always visible) ──
        var nameCol = table.addColumn("name", Contact::name);
        nameCol.setLabel("Name");
        nameCol.setMinWidth(150);
        nameCol.setPinDirection(Column.PinDirection.LEFT);

        // ── Company (hidden on xs) ──
        companyCol = table.addColumn("company", Contact::company);
        companyCol.setLabel("Company");
        companyCol.setMinWidth(160);

        // ── Email (hidden on xs + sm) ──
        emailCol = table.addColumn("email", Contact::email);
        emailCol.setLabel("Email");
        emailCol.setMinWidth(180);

        // ── Phone (hidden on xs + sm) ──
        phoneCol = table.addColumn("phone", Contact::phone);
        phoneCol.setLabel("Phone");
        phoneCol.setMinWidth(130);

        // ── Type badge ──
        var typeCol = table.addColumn("type", Contact::type);
        typeCol.setLabel("Type");
        typeCol.setMinWidth(110);
        typeCol.setSortable(false);
        typeCol.setRenderer(new ConditionalRenderer<Contact>()
                .when("CUSTOMER", new BadgeRenderer<>(BadgeTheme.PRIMARY))
                .when("SUPPLIER", new BadgeRenderer<>(BadgeTheme.SUCCESS))
                .when("EMPLOYEE", new BadgeRenderer<>(BadgeTheme.WARNING))
                .otherwise(new BadgeRenderer<>(BadgeTheme.DEFAULT)));

        // ── Status badge (hidden on xs) ──
        var statusCol = table.addColumn("status", Contact::status);
        statusCol.setLabel("Status");
        statusCol.setMinWidth(90);
        statusCol.setSortable(false);
        statusCol.setRenderer(new ConditionalRenderer<Contact>()
                .when("ACTIVE", new BadgeRenderer<>(BadgeTheme.SUCCESS))
                .otherwise(new BadgeRenderer<>(BadgeTheme.DEFAULT)));

        table.onItemClick(ev -> {
            Contact c = ev.getItem();
            if (c != null) {
                Toast.show("📋  %s  ·  %s  ·  %s".formatted(c.name(), c.company(), c.email()),
                        4000, Theme.PRIMARY, Toast.Placement.BOTTOM_RIGHT);
            }
        });

        return table;
    }

    // ── Pagination Bar ──────────────────────────────────────────────────────────
    private FlexLayout buildPaginationBar() {
        pageInfoText = new Paragraph("Showing 0 contacts");
        pageInfoText.setStyle("margin", "0");
        pageInfoText.setStyle("font-size", ".85rem");
        pageInfoText.setStyle("color", "var(--dwc-color-gray-text-light)");

        navigator = new Navigator(0, 10, Navigator.Layout.PAGES);
        navigator.setAutoDisable(true);
        navigator.setExpanse(Expanse.SMALL);
        navigator.onChange(ev -> onPageChange(ev.getStartIndex(), ev.getEndIndex()));

        pageSizeSelect = new ChoiceBox();
        pageSizeSelect.add("5", "5 / page");
        pageSizeSelect.add("10", "10 / page");
        pageSizeSelect.add("20", "20 / page");
        pageSizeSelect.add("ALL", "All (Scroll)");
        pageSizeSelect.selectKey("10");
        pageSizeSelect.setStyle("width", "130px");
        pageSizeSelect.onSelect(ev -> {
            if (ev.getSelectedItem() != null) {
                onPageSizeChange(ev.getSelectedItem().getKey().toString());
            }
        });

        Paragraph rowsLabel = new Paragraph("Show:");
        rowsLabel.setStyle("margin", "0");
        rowsLabel.setStyle("font-size", ".85rem");
        rowsLabel.setStyle("color", "var(--dwc-color-gray-text-light)");

        FlexLayout sizeWrapper = FlexLayout.create(rowsLabel, pageSizeSelect).align().center().build();
        sizeWrapper.setStyle("gap", ".5rem");

        FlexLayout paginationBar = FlexLayout.create(pageInfoText, navigator, sizeWrapper).build();
        paginationBar.setJustifyContent(FlexJustifyContent.BETWEEN);
        paginationBar.setStyle("align-items", "center");
        paginationBar.setStyle("flex-wrap", "wrap");
        paginationBar.setStyle("gap", "0.75rem");
        paginationBar.setStyle("padding", "0.5rem 0.25rem 0 0.25rem");
        paginationBar.setStyle("border-top", "1px solid var(--dwc-color-default-border)");
        paginationBar.setStyle("flex-shrink", "0");

        return paginationBar;
    }

    private void onPageSizeChange(String sizeKey) {
        applyPagination(true);
    }

    private void onPageChange(int startIndex, int endIndex) {
        int total = allContacts.size();
        if (total == 0) {
            table.setItems(List.of());
            pageInfoText.setText("No contacts");
            return;
        }
        int start = Math.max(0, Math.min(startIndex, total - 1));
        int end = Math.min(endIndex + 1, total);
        if (start < end) {
            table.setItems(allContacts.subList(start, end));
            pageInfoText.setText(String.format("Showing %d–%d of %d contacts", start + 1, end, total));
        }
    }

    private void applyPagination(boolean resetPage) {
        String selected = (pageSizeSelect != null && pageSizeSelect.getSelectedKey() != null)
                ? pageSizeSelect.getSelectedKey().toString()
                : "10";

        int total = allContacts.size();

        if ("ALL".equalsIgnoreCase(selected)) {
            // Optional pagination: when 'All (Scroll)' is chosen, hide navigator and show full dataset with scroll
            if (navigator != null) {
                navigator.setStyle("display", "none");
            }
            table.setItems(allContacts);
            pageInfoText.setText(total == 0 ? "No contacts" : String.format("Showing all %d contacts (scroll enabled)", total));
            return;
        }

        if (navigator != null) {
            navigator.setStyle("display", "flex");
        }

        int pageSize = 10;
        try {
            pageSize = Integer.parseInt(selected);
        } catch (NumberFormatException ignored) {
        }

        if (navigator != null) {
            Paginator paginator = navigator.getPaginator();
            paginator.setSize(pageSize);
            paginator.setTotalItems(total);
            if (resetPage) {
                paginator.setCurrent(1);
            }

            if (total == 0) {
                table.setItems(List.of());
                pageInfoText.setText("No contacts");
            } else {
                int start = paginator.getStartIndex();
                int end = Math.min(paginator.getEndIndex() + 1, total);
                table.setItems(allContacts.subList(start, end));
                pageInfoText.setText(String.format("Showing %d–%d of %d contacts", start + 1, end, total));
            }
        }
    }

    private void injectResponsiveCss() {
        String css = """
        /* ── Contacts table responsive column visibility & scrolling ── */
        dwc-table {
          flex: 1 1 0 !important;
          width: 100% !important;
          min-height: 0 !important;
        }
        @media (max-width: 899px) {
          .ag-header-cell[col-id="company"],
          .ag-cell[col-id="company"],
          .ag-header-cell[col-id="email"],
          .ag-cell[col-id="email"],
          .ag-header-cell[col-id="phone"],
          .ag-cell[col-id="phone"] {
            display: none !important;
          }
        }
        @media (max-width: 639px) {
          .ag-header-cell[col-id="status"],
          .ag-cell[col-id="status"] {
            display: none !important;
          }
        }
        """;
        Page.getCurrent().addInlineStyleSheet(css);
    }

    // ---------------------------------------------------------------------------
    // Add-Contact Dialog
    // ---------------------------------------------------------------------------
    private void openAddDialog() {
        Dialog dialog = new Dialog();
        self.add(dialog);
        dialog.onClose(ev -> self.remove(dialog));
        dialog.setMaxWidth("520px");
        dialog.setCancelOnEscKey(true);
        dialog.setCancelOnOutsideClick(false);

        // ── Dialog header ──
        H2 dialogTitle = new H2("New Contact");
        dialogTitle.setStyle("margin", "0");
        dialogTitle.setStyle("font-size", "1.1rem");
        dialogTitle.setStyle("font-weight", "600");
        dialog.addToHeader(dialogTitle);

        // ── Form fields ──
        TextField nameField = field("Full name *", "Ana Tavares");
        TextField emailField = field("Email", "ana@example.cv");
        TextField phoneField = field("Phone", "+238 991 2345");
        TextField companyField = field("Company", "Mercado Sucupira");

        ChoiceBox typeBox = new ChoiceBox();
        typeBox.add("CUSTOMER", "Customer");
        typeBox.add("SUPPLIER", "Supplier");
        typeBox.add("EMPLOYEE", "Employee");
        typeBox.selectKey("CUSTOMER");
        typeBox.setStyle("width", "100%");

        Paragraph typeLabel = new Paragraph("Type");
        typeLabel.setStyle("font-size", ".8rem");
        typeLabel.setStyle("font-weight", "600");
        typeLabel.setStyle("margin", "0 0 4px 0");
        typeLabel.setStyle("color", "var(--dwc-color-gray-text-light)");

        FlexLayout typeWrapper = FlexLayout.create(typeLabel, typeBox).vertical().build();
        typeWrapper.setStyle("gap", "4px");

        FlexLayout row1 = FlexLayout.create(nameField, emailField).build();
        row1.setStyle("gap", ".75rem");
        row1.setStyle("flex-wrap", "wrap");

        FlexLayout row2 = FlexLayout.create(phoneField, companyField).build();
        row2.setStyle("gap", ".75rem");
        row2.setStyle("flex-wrap", "wrap");

        FlexLayout form = FlexLayout.create(row1, row2, typeWrapper).vertical().build();
        form.setStyle("gap", ".85rem");
        form.setStyle("padding", ".25rem 0");
        dialog.addToContent(form);

        // ── Footer ──
        Button cancelBtn = new Button("Cancel").setTheme(ButtonTheme.DEFAULT);
        Button saveBtn = new Button("Save contact")
                .setPrefixComponent(TablerIcon.create("check"))
                .setTheme(ButtonTheme.PRIMARY);

        cancelBtn.onClick(ev -> dialog.close());
        saveBtn.onClick(ev -> handleSave(dialog, nameField, emailField, phoneField, companyField, typeBox));

        FlexLayout footer = FlexLayout.create(cancelBtn, saveBtn).build();
        footer.setJustifyContent(FlexJustifyContent.END);
        footer.setStyle("gap", ".5rem");
        footer.setStyle("padding-top", ".5rem");
        dialog.addToFooter(footer);

        dialog.open();
    }

    private void handleSave(Dialog dialog, TextField nameField, TextField emailField,
            TextField phoneField, TextField companyField, ChoiceBox typeBox) {

        String name = nvl(nameField.getValue());
        if (name.isEmpty()) {
            Toast.show("Name is required", 3000, Theme.DANGER, Toast.Placement.BOTTOM_RIGHT);
            return;
        }

        String type = typeBox.getSelectedKey() == null
                ? "CUSTOMER" : typeBox.getSelectedKey().toString();

        Contact saved = (repository != null)
                ? repository.save(new Contact(null, name,
                        nvl(emailField.getValue()),
                        nvl(phoneField.getValue()),
                        nvl(companyField.getValue()),
                        type, "ACTIVE"))
                : null;

        dialog.close();

        if (saved != null) {
            refreshTable(searchField.getValue());
            Toast.show("✅  %s added".formatted(saved.name()),
                    4000, Theme.SUCCESS, Toast.Placement.BOTTOM_RIGHT);
        }
    }

    // ---------------------------------------------------------------------------
    // Data helpers
    // ---------------------------------------------------------------------------
    private void refreshTable(String query) {
        if (repository == null) {
            return;
        }
        allContacts = repository.search(query);
        updateStats();
        applyPagination(true);
    }

    private void updateStats() {
        if (repository != null) {
            if (customersCountLabel != null) {
                customersCountLabel.setText(String.valueOf(repository.countByType("CUSTOMER")));
            }
            if (suppliersCountLabel != null) {
                suppliersCountLabel.setText(String.valueOf(repository.countByType("SUPPLIER")));
            }
            if (employeesCountLabel != null) {
                employeesCountLabel.setText(String.valueOf(repository.countByType("EMPLOYEE")));
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Utilities
    // ---------------------------------------------------------------------------
    private TextField field(String label, String placeholder) {
        TextField f = new TextField();
        f.setLabel(label);
        f.setPlaceholder(placeholder);
        f.setStyle("flex", "1");
        f.setStyle("min-width", "180px");
        return f;
    }

    private String nvl(String s) {
        return s == null ? "" : s.trim();
    }
}
