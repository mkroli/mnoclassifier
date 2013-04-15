###
MnoClassifier learns MSISDN-Operator combinations to afterwards predict Operators.
Copyright (C) 2013 MACH Connectivity GmbH

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
###

angular.module('classificationService', ['ngResource']).factory 'Classification', ($resource) ->
	$resource('/api/:msisdn', {}, (classify: (method: "GET", params: (msisdn: "msisdn"))))

window.ClassificationController = ($scope, $routeParams, $location, Classification) ->
	$scope.msisdn = $routeParams.msisdn || ''

	$scope.result = (results: [], relevance: 0.0)

	$scope.displayResults = () -> $scope.operatorPages.length > 0

	$scope.textAlign = () -> if $scope.displayResults() then '' else 'center'

	$scope.operatorPages = []

	$scope.page = 0

	$scope.previousPage = () -> $scope.page = Math.max(0, $scope.page - 1)

	$scope.nextPage = () -> $scope.page = Math.min($scope.operatorPages.length - 1, $scope.page + 1)

	$scope.setPage = (page) -> $scope.page = page

	$scope.isActivePage = (page) -> $scope.page == page

	$scope.isPreviousEnabled = () -> $scope.page <= 0

	$scope.isNextEnabled = () -> $scope.page >= ($scope.operatorPages.length - 1)

	$scope.confidenceString = () -> (Math.floor($scope.result.confidence * 10000) / 100) + "%"

	$scope.confidenceClass = () ->
		if $scope.confidence < 0.75
			"bar-danger"
		else if $scope.confidence < 0.9
			"bar-warning"
		else
			"bar-success"

	$scope.isLoading = false

	$scope.classify = () ->
		$scope.isLoading = true
		$scope.result = Classification.classify (msisdn: $scope.msisdn), () ->
			$scope.isLoading = false
			$scope.operatorPages = $scope.result.results.inGroupsOf(5).compact()
		$location.path('/classification/' + $scope.msisdn);

	if $scope.msisdn.match(/\d+/)
		$scope.classify()
