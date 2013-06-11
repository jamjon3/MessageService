'use strict';

angular.module('angularApp')
  .controller('DashboardCtrl', function($scope) {
  var chart1 = {};
  chart1.type = 'AreaChart';
  chart1.displayed = false;
  chart1.cssStyle = 'height:600px; width:100%;';
  chart1.data = {'cols': [
      {id: 'type', label: 'Type', type: 'string'},
      {id: 'queue-id', label: 'Queue Messages', type: 'number'},
      {id: 'topic-id', label: 'Topic Messages', type: 'number'}
    ], 'rows': [
      {c: [
          {v: 'Queue'},
          {v: 3465}
        ]},
        {c: [
          {v: 'Topic'},
          {v: 3464}
        ]}
      ]};

  chart1.options = {
      'title': 'Sales per month',
      'isStacked': 'true',
      'fill': 20,
      'displayExactValues': true,
      'vAxis': {
        'title': 'Sales unit',
        'gridlines': {'count': 10}
      },
      'hAxis': {
          'title': 'Date'
        }
      };

  $scope.chart = chart1;
});