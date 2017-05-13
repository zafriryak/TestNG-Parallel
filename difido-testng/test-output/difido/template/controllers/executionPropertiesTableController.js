function executionPropertiesTableController(table) {
	var properties = collectAllScenarioProperties();

	for ( var key in properties) {
		var tr = $('<tr>');
		if (key !== "duration") {
			tr.append($('<td>').text(key));
			tr.append($('<td>').text(properties[key]));
			$(table).append(tr);
		}
	};
	
}